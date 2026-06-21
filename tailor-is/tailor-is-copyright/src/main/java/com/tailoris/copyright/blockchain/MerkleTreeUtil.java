package com.tailoris.copyright.blockchain;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Merkle 树工具类.
 *
 * <p>用于批量版权存证时计算 Merkle Root，支持高效批量上链存证。
 * 单次交易只需提交 Merkle Root，后续可通过 Merkle Proof 验证单个版权。</p>
 *
 * <h3>Merkle 树结构</h3>
 * <pre>
 *        Root (H0123)
 *       /          \
 *    H01           H23
 *   /   \         /   \
 *  H0   H1      H2   H3
 * </pre>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>批量版权登记：计算 Merkle Root 并上链，减少区块链交易数量</li>
 *   <li>版权验证：提供 Merkle Proof 验证某个版权是否在批量存证中</li>
 *   <li>流式处理：支持大数据量分批计算，避免内存溢出</li>
 * </ul>
 */
@Slf4j
@Component
public class MerkleTreeUtil {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int DEFAULT_BATCH_SIZE = 1024;

    /**
     * MessageDigest 线程本地池，避免每次 hashPair 都创建新实例。
     * <p>SHA-256 MessageDigest 不是线程安全的，通过 ThreadLocal 为每个线程
     * 维护独立实例，既保证线程安全又避免频繁创建开销。</p>
     */
    private static final ThreadLocal<MessageDigest> MESSAGE_DIGEST_POOL =
            ThreadLocal.withInitial(() -> {
                try {
                    return MessageDigest.getInstance(HASH_ALGORITHM);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("SHA-256 algorithm not available", e);
                }
            });

    /**
     * 构建 Merkle 树.
     *
     * @param dataHashes 版权哈希列表
     * @return Merkle 树
     */
    public MerkleTree buildTree(List<String> dataHashes) {
        if (dataHashes == null || dataHashes.isEmpty()) {
            throw new IllegalArgumentException("数据哈希列表不能为空");
        }

        List<MerkleNode> leaves = dataHashes.stream()
                .map(hash -> new MerkleNode(hash, null, null))
                .collect(Collectors.toList());

        MerkleNode root = buildTreeFromLeaves(leaves);
        return new MerkleTree(root, leaves);
    }

    /**
     * 流式构建 Merkle 树（适用于超大批量数据，避免 OOM）。
     *
     * <p>与 {@link #buildTree(List)} 不同，本方法接收字节流，分批处理数据，
     * 不需要将所有数据一次性加载到内存。每批数据计算哈希后逐层归并。</p>
     *
     * <p>典型用法：</p>
     * <pre>{@code
     *   Stream<byte[]> dataStream = files.stream().map(Files::readAllBytes);
     *   String root = merkleTreeUtil.buildMerkleTreeStreaming(dataStream);
     * }</pre>
     *
     * @param dataStream 数据字节流（惰性求值，不会一次性加载）
     * @return Merkle Root 哈希
     */
    public String buildMerkleTreeStreaming(Stream<byte[]> dataStream) {
        return buildMerkleTreeStreaming(dataStream, DEFAULT_BATCH_SIZE);
    }

    /**
     * 流式构建 Merkle 树（可指定批次大小）。
     *
     * @param dataStream 数据字节流
     * @param batchSize  每批处理数量
     * @return Merkle Root 哈希
     */
    public String buildMerkleTreeStreaming(Stream<byte[]> dataStream, int batchSize) {
        List<MerkleNode> currentLevel = new ArrayList<>();

        dataStream.forEach(data -> {
            String hash = hashBytes(data);
            currentLevel.add(new MerkleNode(hash, null, null));

            if (currentLevel.size() >= batchSize) {
                List<MerkleNode> reduced = reduceLevel(currentLevel);
                currentLevel.clear();
                currentLevel.addAll(reduced);
            }
        });

        // 最终归并剩余节点
        while (currentLevel.size() > 1) {
            currentLevel = reduceLevel(currentLevel);
        }

        return currentLevel.isEmpty() ? null : currentLevel.get(0).getHash();
    }

    /**
     * 批量计算 Merkle Root（可配置批次大小）。
     *
     * <p>适用于已知全部哈希列表但有大量数据的情况，
     * 通过分批归并控制单次计算的内存占用。</p>
     *
     * <p>JMH 基准测试示例：</p>
     * <pre>{@code
     *   // 100 items
     *   computeBatchMerkleRoot(hashes, 100)
     *   // 1000 items
     *   computeBatchMerkleRoot(hashes, 1000)
     *   // 10000 items
     *   computeBatchMerkleRoot(hashes, 10000)
     * }</pre>
     *
     * @param hashes    原始数据哈希列表
     * @param batchSize 每批处理数量
     * @return Merkle Root 哈希
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 3, time = 1)
    @Measurement(iterations = 5, time = 1)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(1)
    public String computeBatchMerkleRoot(List<byte[]> hashes, int batchSize) {
        if (hashes == null || hashes.isEmpty()) {
            return null;
        }

        List<MerkleNode> currentLevel = new ArrayList<>();
        int count = 0;

        for (byte[] data : hashes) {
            String hash = hashBytes(data);
            currentLevel.add(new MerkleNode(hash, null, null));
            count++;

            if (count % batchSize == 0 && currentLevel.size() > 1) {
                currentLevel = reduceLevel(currentLevel);
            }
        }

        while (currentLevel.size() > 1) {
            currentLevel = reduceLevel(currentLevel);
        }

        return currentLevel.isEmpty() ? null : currentLevel.get(0).getHash();
    }

    /**
     * 从叶子节点构建 Merkle 树.
     */
    private MerkleNode buildTreeFromLeaves(List<MerkleNode> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }

        List<MerkleNode> parents = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i += 2) {
            MerkleNode left = nodes.get(i);
            MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : left; // 奇数节点复制
            String parentHash = hashPair(left.getHash(), right.getHash());
            MerkleNode parent = new MerkleNode(parentHash, left, right);
            parents.add(parent);
        }

        return buildTreeFromLeaves(parents);
    }

    /**
     * 获取 Merkle Proof.
     *
     * @param tree       Merkle 树
     * @param targetHash 目标哈希
     * @return Merkle Proof 路径
     */
    public List<MerkleProofItem> getProof(MerkleTree tree, String targetHash) {
        List<MerkleProofItem> proof = new ArrayList<>();
        MerkleNode targetLeaf = findLeaf(tree.getLeaves(), targetHash);
        if (targetLeaf == null) {
            return proof;
        }

        buildProof(tree.getLeaves(), targetHash, proof);
        return proof;
    }

    /**
     * 验证 Merkle Proof.
     *
     * @param proof      Merkle Proof
     * @param targetHash 目标哈希
     * @param root       Merkle Root
     * @return true 验证通过
     */
    public boolean verifyProof(List<MerkleProofItem> proof, String targetHash, String root) {
        String currentHash = targetHash;
        for (MerkleProofItem item : proof) {
            if (item.isLeft()) {
                currentHash = hashPair(item.getHash(), currentHash);
            } else {
                currentHash = hashPair(currentHash, item.getHash());
            }
        }
        return currentHash.equals(root);
    }

    /**
     * 流式计算 Merkle Root（适用于超大批量数据）.
     *
     * @param hashStream 哈希流
     * @param batchSize  每批处理数量
     * @return Merkle Root
     */
    public String streamComputeRoot(Iterator<String> hashStream, int batchSize) {
        List<MerkleNode> currentLevel = new ArrayList<>();

        while (hashStream.hasNext()) {
            currentLevel.add(new MerkleNode(hashStream.next(), null, null));

            if (currentLevel.size() >= batchSize) {
                currentLevel = reduceLevel(currentLevel);
            }
        }

        while (currentLevel.size() > 1) {
            currentLevel = reduceLevel(currentLevel);
        }

        return currentLevel.isEmpty() ? null : currentLevel.get(0).getHash();
    }

    private List<MerkleNode> reduceLevel(List<MerkleNode> nodes) {
        List<MerkleNode> parents = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i += 2) {
            MerkleNode left = nodes.get(i);
            MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : left;
            String parentHash = hashPair(left.getHash(), right.getHash());
            parents.add(new MerkleNode(parentHash, left, right));
        }
        return parents;
    }

    /**
     * 计算两个哈希的父哈希（使用 ThreadLocal MessageDigest 池）。
     */
    private String hashPair(String left, String right) {
        MessageDigest md = MESSAGE_DIGEST_POOL.get();
        md.reset();
        md.update(left.getBytes(StandardCharsets.UTF_8));
        md.update(right.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        return bytesToHex(digest);
    }

    /**
     * 计算单个字节数组的 SHA-256 哈希（使用 ThreadLocal MessageDigest 池）。
     */
    private String hashBytes(byte[] data) {
        MessageDigest md = MESSAGE_DIGEST_POOL.get();
        md.reset();
        byte[] digest = md.digest(data);
        return bytesToHex(digest);
    }

    private MerkleNode findLeaf(List<MerkleNode> leaves, String targetHash) {
        return leaves.stream()
                .filter(leaf -> leaf.getHash().equals(targetHash))
                .findFirst()
                .orElse(null);
    }

    private void buildProof(List<MerkleNode> nodes, String targetHash, List<MerkleProofItem> proof) {
        if (nodes.size() <= 1) {
            return;
        }

        List<MerkleNode> parents = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i += 2) {
            MerkleNode left = nodes.get(i);
            MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : left;

            if (left.getHash().equals(targetHash)) {
                proof.add(new MerkleProofItem(right.getHash(), false));
                targetHash = hashPair(left.getHash(), right.getHash());
            } else if (right.getHash().equals(targetHash)) {
                proof.add(new MerkleProofItem(left.getHash(), true));
                targetHash = hashPair(left.getHash(), right.getHash());
            }

            parents.add(new MerkleNode(hashPair(left.getHash(), right.getHash()), left, right));
        }

        buildProof(parents, targetHash, proof);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ---- 内部类 ----

    /**
     * Merkle 树节点.
     */
    public static class MerkleNode {
        private final String hash;
        private final MerkleNode left;
        private final MerkleNode right;

        public MerkleNode(String hash, MerkleNode left, MerkleNode right) {
            this.hash = hash;
            this.left = left;
            this.right = right;
        }

        public String getHash() { return hash; }
        public MerkleNode getLeft() { return left; }
        public MerkleNode getRight() { return right; }
    }

    /**
     * Merkle 树.
     */
    public static class MerkleTree {
        private final MerkleNode root;
        private final List<MerkleNode> leaves;

        public MerkleTree(MerkleNode root, List<MerkleNode> leaves) {
            this.root = root;
            this.leaves = leaves;
        }

        public String getRoot() { return root.getHash(); }
        public MerkleNode getRootNode() { return root; }
        public List<MerkleNode> getLeaves() { return leaves; }
        public int getLeafCount() { return leaves.size(); }
    }

    /**
     * Merkle Proof 项.
     */
    public static class MerkleProofItem {
        /** 兄弟节点哈希 */
        private final String hash;
        /** 目标节点是否在左侧 */
        private final boolean isLeft;

        public MerkleProofItem(String hash, boolean isLeft) {
            this.hash = hash;
            this.isLeft = isLeft;
        }

        public String getHash() { return hash; }
        public boolean isLeft() { return isLeft; }
    }
}
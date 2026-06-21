package com.tailoris.copyright.blockchain;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * MerkleTreeUtil JMH 基准测试.
 *
 * <p>测试场景：</p>
 * <ul>
 *   <li>100 条数据：小批量版权登记</li>
 *   <li>1000 条数据：中批量版权登记</li>
 *   <li>10000 条数据：大批量版权登记</li>
 * </ul>
 *
 * <p>对比维度：</p>
 * <ul>
 *   <li>非流式 {@link MerkleTreeUtil#buildTree(List)}</li>
 *   <li>流式 {@link MerkleTreeUtil#buildMerkleTreeStreaming(Stream)}</li>
 *   <li>批量 {@link MerkleTreeUtil#computeBatchMerkleRoot(List, int)}</li>
 * </ul>
 *
 * <p>运行方式：</p>
 * <pre>
 *   mvn test -pl tailor-is-copyright -Dtest=MerkleTreeUtilBenchmark
 *   # 或直接运行 main 方法
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class MerkleTreeUtilBenchmark {

    private MerkleTreeUtil util;

    private List<byte[]> data100;
    private List<byte[]> data1000;
    private List<byte[]> data10000;

    private List<String> hash100;
    private List<String> hash1000;
    private List<String> hash10000;

    @Setup(Level.Trial)
    public void setup() {
        util = new MerkleTreeUtil();
        SecureRandom random = new SecureRandom();

        data100 = generateData(100, random);
        data1000 = generateData(1000, random);
        data10000 = generateData(10000, random);

        hash100 = data100.stream()
                .map(b -> bytesToHex(sha256(b)))
                .toList();
        hash1000 = data1000.stream()
                .map(b -> bytesToHex(sha256(b)))
                .toList();
        hash10000 = data10000.stream()
                .map(b -> bytesToHex(sha256(b)))
                .toList();
    }

    private List<byte[]> generateData(int count, SecureRandom random) {
        List<byte[]> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] data = new byte[256];
            random.nextBytes(data);
            list.add(data);
        }
        return list;
    }

    private static byte[] sha256(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ==================== 100 items ====================

    @Benchmark
    public void buildTree_100(Blackhole bh) {
        MerkleTreeUtil.MerkleTree tree = util.buildTree(hash100);
        bh.consume(tree.getRoot());
    }

    @Benchmark
    public void streaming_100(Blackhole bh) {
        String root = util.buildMerkleTreeStreaming(data100.stream());
        bh.consume(root);
    }

    @Benchmark
    public void batch_100(Blackhole bh) {
        String root = util.computeBatchMerkleRoot(data100, 50);
        bh.consume(root);
    }

    // ==================== 1000 items ====================

    @Benchmark
    public void buildTree_1000(Blackhole bh) {
        MerkleTreeUtil.MerkleTree tree = util.buildTree(hash1000);
        bh.consume(tree.getRoot());
    }

    @Benchmark
    public void streaming_1000(Blackhole bh) {
        String root = util.buildMerkleTreeStreaming(data1000.stream());
        bh.consume(root);
    }

    @Benchmark
    public void batch_1000(Blackhole bh) {
        String root = util.computeBatchMerkleRoot(data1000, 500);
        bh.consume(root);
    }

    // ==================== 10000 items ====================

    @Benchmark
    public void buildTree_10000(Blackhole bh) {
        MerkleTreeUtil.MerkleTree tree = util.buildTree(hash10000);
        bh.consume(tree.getRoot());
    }

    @Benchmark
    public void streaming_10000(Blackhole bh) {
        String root = util.buildMerkleTreeStreaming(data10000.stream());
        bh.consume(root);
    }

    @Benchmark
    public void batch_10000(Blackhole bh) {
        String root = util.computeBatchMerkleRoot(data10000, 5000);
        bh.consume(root);
    }

    // ==================== 不同 batchSize 对比 ====================

    @Benchmark
    public void batch_10000_size_500(Blackhole bh) {
        String root = util.computeBatchMerkleRoot(data10000, 500);
        bh.consume(root);
    }

    @Benchmark
    public void batch_10000_size_2000(Blackhole bh) {
        String root = util.computeBatchMerkleRoot(data10000, 2000);
        bh.consume(root);
    }

    /**
     * 可直接运行的 main 方法，用于 IDE 中快速验证。
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MerkleTreeUtilBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(3)
                .build();

        new Runner(opt).run();
    }
}
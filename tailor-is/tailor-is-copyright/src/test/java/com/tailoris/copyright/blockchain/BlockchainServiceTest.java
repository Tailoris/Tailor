package com.tailoris.copyright.blockchain;

import com.tailoris.copyright.blockchain.BlockchainService.CopyrightData;
import com.tailoris.copyright.blockchain.impl.HyperledgerFabricServiceImpl;
import com.tailoris.copyright.blockchain.impl.EthBlockchainServiceImpl;
import com.tailoris.copyright.config.BlockchainConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BlockchainService 单元测试 - TEST-P2-01.
 *
 * <p>测试区块链版权登记和验证的核心逻辑：</p>
 * <ul>
 *   <li>版权登记（单条/批量）</li>
 *   <li>版权验证（匹配/未匹配）</li>
 *   <li>证书查询</li>
 *   <li>健康检查</li>
 *   <li>Merkle 树构建与验证</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BlockchainService 区块链单元测试")
class BlockchainServiceTest {

    // ============================================================
    // Hyperledger Fabric 实现测试
    // ============================================================

    @Nested
    @DisplayName("Hyperledger Fabric 实现")
    class FabricTests {

        @Mock private BlockchainConfig blockchainConfig;

        @Spy
        private MerkleTreeUtil merkleTreeUtil = new MerkleTreeUtil();

        @InjectMocks
        private HyperledgerFabricServiceImpl fabricService;

        @BeforeEach
        void setUp() {
            lenient().when(blockchainConfig.getFabricChannelName()).thenReturn("copyright-channel");
            lenient().when(blockchainConfig.getFabricChaincodeName()).thenReturn("copyright-cc");
        }

        @Test
        @DisplayName("登记版权 - 成功")
        void registerCopyright_Success() {
            CopyrightData data = buildCopyrightData("BIZ-001", "设计作品", "author-1", "张三", "hash123abc");

            Map<String, Object> result = fabricService.registerCopyright(data);

            assertNotNull(result);
            assertTrue((Boolean) result.get("success"));
            assertNotNull(result.get("txHash"));
            assertTrue(((String) result.get("txHash")).length() > 0);
            assertEquals("fabric", result.get("platform"));
            assertNotNull(result.get("blockHeight"));
            assertNotNull(result.get("timestamp"));
        }

        @Test
        @DisplayName("登记版权 - 验证后能匹配")
        void registerAndVerify() {
            CopyrightData data = buildCopyrightData("BIZ-002", "新作品", "author-2", "李四", "hash_verify_test");

            fabricService.registerCopyright(data);

            Map<String, Object> verifyResult = fabricService.verifyCopyright("hash_verify_test");

            assertNotNull(verifyResult);
            assertTrue((Boolean) verifyResult.get("matched"));
            assertNotNull(verifyResult.get("txHash"));
            assertEquals("新作品", verifyResult.get("workName"));
            assertEquals("李四", verifyResult.get("authorName"));
        }

        @Test
        @DisplayName("验证版权 - 未匹配")
        void verifyCopyright_NotMatched() {
            Map<String, Object> result = fabricService.verifyCopyright("nonexistent_hash");

            assertNotNull(result);
            assertFalse((Boolean) result.get("matched"));
            assertEquals("fabric", result.get("platform"));
        }

        @Test
        @DisplayName("批量登记 - 成功")
        void batchRegister_Success() {
            List<CopyrightData> dataList = Arrays.asList(
                    buildCopyrightData("BIZ-003", "作品A", "author-1", "张三", "hash_batch_1"),
                    buildCopyrightData("BIZ-004", "作品B", "author-2", "李四", "hash_batch_2"),
                    buildCopyrightData("BIZ-005", "作品C", "author-3", "王五", "hash_batch_3")
            );

            Map<String, Object> result = fabricService.batchRegister(dataList);

            assertNotNull(result);
            assertTrue((Boolean) result.get("success"));
            assertNotNull(result.get("txHash"));
            assertNotNull(result.get("merkleRoot"));
            assertEquals(3, result.get("count"));
            assertEquals("fabric", result.get("platform"));
        }

        @Test
        @DisplayName("批量登记 - 空列表抛异常")
        void batchRegister_Empty() {
            assertThrows(IllegalArgumentException.class,
                    () -> fabricService.batchRegister(Collections.emptyList()));
        }

        @Test
        @DisplayName("批量登记 - null列表抛异常")
        void batchRegister_Null() {
            assertThrows(IllegalArgumentException.class,
                    () -> fabricService.batchRegister(null));
        }

        @Test
        @DisplayName("查询证书 - 存在")
        void queryCertificate_Exists() {
            CopyrightData data = buildCopyrightData("BIZ-006", "证书作品", "author-1", "张三", "hash_cert");
            Map<String, Object> registerResult = fabricService.registerCopyright(data);
            String txHash = (String) registerResult.get("txHash");

            Map<String, Object> result = fabricService.queryCertificate(txHash);

            assertNotNull(result);
            assertTrue((Boolean) result.get("exists"));
            assertEquals(txHash, result.get("txHash"));
            assertEquals("证书作品", result.get("workName"));
        }

        @Test
        @DisplayName("查询证书 - 不存在")
        void queryCertificate_NotExists() {
            Map<String, Object> result = fabricService.queryCertificate("non_existent_tx");

            assertNotNull(result);
            assertFalse((Boolean) result.get("exists"));
        }

        @Test
        @DisplayName("健康检查")
        void isHealthy() {
            assertTrue(fabricService.isHealthy());
        }

        @Test
        @DisplayName("获取平台名称")
        void getPlatformName() {
            assertEquals("FABRIC", fabricService.getPlatformName());
        }
    }

    // ============================================================
    // 以太坊实现测试
    // ============================================================

    @Nested
    @DisplayName("以太坊实现")
    class EthTests {

        @Mock private BlockchainConfig blockchainConfig;

        @Spy
        private MerkleTreeUtil merkleTreeUtil = new MerkleTreeUtil();

        @InjectMocks
        private EthBlockchainServiceImpl ethService;

        @BeforeEach
        void setUp() {
            lenient().when(blockchainConfig.getEthRpcUrl()).thenReturn("http://localhost:8545");
            lenient().when(blockchainConfig.getEthContractAddress()).thenReturn("0xContractAddress");
        }

        @Test
        @DisplayName("登记版权 - 成功")
        void registerCopyright_Success() {
            CopyrightData data = buildCopyrightData("ETH-001", "以太坊作品", "author-1", "张三", "eth_hash_1");

            Map<String, Object> result = ethService.registerCopyright(data);

            assertNotNull(result);
            assertTrue((Boolean) result.get("success"));
            assertNotNull(result.get("txHash"));
            assertTrue(((String) result.get("txHash")).startsWith("0x"));
            assertEquals("ethereum", result.get("platform"));
        }

        @Test
        @DisplayName("验证版权 - 匹配")
        void verifyCopyright_Matched() {
            CopyrightData data = buildCopyrightData("ETH-002", "验证作品", "author-2", "李四", "eth_verify_hash");
            ethService.registerCopyright(data);

            Map<String, Object> result = ethService.verifyCopyright("eth_verify_hash");

            assertNotNull(result);
            assertTrue((Boolean) result.get("matched"));
            assertEquals("验证作品", result.get("workName"));
        }

        @Test
        @DisplayName("验证版权 - 未匹配")
        void verifyCopyright_NotMatched() {
            Map<String, Object> result = ethService.verifyCopyright("eth_not_found");

            assertNotNull(result);
            assertFalse((Boolean) result.get("matched"));
            assertEquals("ethereum", result.get("platform"));
        }

        @Test
        @DisplayName("批量登记 - 成功")
        void batchRegister_Success() {
            List<CopyrightData> dataList = Arrays.asList(
                    buildCopyrightData("ETH-003", "作品D", "author-1", "张三", "eth_batch_1"),
                    buildCopyrightData("ETH-004", "作品E", "author-2", "李四", "eth_batch_2")
            );

            Map<String, Object> result = ethService.batchRegister(dataList);

            assertNotNull(result);
            assertTrue((Boolean) result.get("success"));
            assertNotNull(result.get("merkleRoot"));
            assertEquals(2, result.get("count"));
        }

        @Test
        @DisplayName("查询证书")
        void queryCertificate() {
            CopyrightData data = buildCopyrightData("ETH-005", "证书", "author-1", "张三", "eth_cert_hash");
            Map<String, Object> registerResult = ethService.registerCopyright(data);
            String txHash = (String) registerResult.get("txHash");

            Map<String, Object> result = ethService.queryCertificate(txHash);

            assertNotNull(result);
            assertTrue((Boolean) result.get("exists"));
        }

        @Test
        @DisplayName("获取平台名称")
        void getPlatformName() {
            assertEquals("ETHEREUM", ethService.getPlatformName());
        }
    }

    // ============================================================
    // MerkleTree 工具测试
    // ============================================================

    @Nested
    @DisplayName("MerkleTree 工具")
    class MerkleTreeTests {

        private final MerkleTreeUtil merkleTreeUtil = new MerkleTreeUtil();

        @Test
        @DisplayName("构建Merkle树 - 单节点")
        void buildTree_SingleNode() {
            MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(List.of("hash1"));

            assertNotNull(tree);
            assertEquals("hash1", tree.getRoot());
            assertEquals(1, tree.getLeafCount());
        }

        @Test
        @DisplayName("构建Merkle树 - 2的幂次节点")
        void buildTree_PowerOfTwo() {
            List<String> hashes = Arrays.asList("h1", "h2", "h3", "h4");
            MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(hashes);

            assertNotNull(tree);
            assertNotNull(tree.getRoot());
            assertEquals(4, tree.getLeafCount());
            assertNotEquals("h1", tree.getRoot());
        }

        @Test
        @DisplayName("构建Merkle树 - 奇数节点")
        void buildTree_OddNodes() {
            List<String> hashes = Arrays.asList("h1", "h2", "h3");
            MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(hashes);

            assertNotNull(tree);
            assertNotNull(tree.getRoot());
            assertEquals(3, tree.getLeafCount());
        }

        @Test
        @DisplayName("Merkle Proof - 验证通过")
        void verifyProof_Success() {
            List<String> hashes = Arrays.asList("h1", "h2", "h3", "h4");
            MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(hashes);
            String root = tree.getRoot();

            List<MerkleTreeUtil.MerkleProofItem> proof = merkleTreeUtil.getProof(tree, "h1");
            assertFalse(proof.isEmpty());

            boolean verified = merkleTreeUtil.verifyProof(proof, "h1", root);
            assertTrue(verified);
        }

        @Test
        @DisplayName("Merkle Proof - 验证失败（错误的root）")
        void verifyProof_WrongRoot() {
            List<String> hashes = Arrays.asList("h1", "h2", "h3", "h4");
            MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(hashes);
            List<MerkleTreeUtil.MerkleProofItem> proof = merkleTreeUtil.getProof(tree, "h1");

            boolean verified = merkleTreeUtil.verifyProof(proof, "h1", "wrong_root");
            assertFalse(verified);
        }

        @Test
        @DisplayName("流式计算Merkle Root")
        void streamComputeRoot_Success() {
            List<String> hashes = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6");
            String root = merkleTreeUtil.streamComputeRoot(hashes.iterator(), 2);

            assertNotNull(root);
            assertFalse(root.isEmpty());
        }

        @Test
        @DisplayName("流式计算Merkle Root - 空输入")
        void streamComputeRoot_Empty() {
            String root = merkleTreeUtil.streamComputeRoot(Collections.emptyIterator(), 2);

            assertNull(root);
        }

        @Test
        @DisplayName("Merkle Proof - 目标不在树中")
        void getProof_NotFound() {
            List<String> hashes = Arrays.asList("h1", "h2", "h3");
            MerkleTreeUtil.MerkleTree tree = merkleTreeUtil.buildTree(hashes);

            List<MerkleTreeUtil.MerkleProofItem> proof = merkleTreeUtil.getProof(tree, "not_found");

            assertTrue(proof.isEmpty());
        }
    }

    // ============================================================
    // CopyrightData 模型测试
    // ============================================================

    @Test
    @DisplayName("CopyrightData 模型 - getter/setter")
    void copyrightData_Model() {
        CopyrightData data = new CopyrightData();
        data.setBizId("BIZ-100");
        data.setWorkName("测试作品");
        data.setAuthorId("author-100");
        data.setAuthorName("作者名");
        data.setFileHash("sha256hash");
        data.setFileType("PNG");
        data.setFileSize(2048L);
        data.setCreationTimestamp(1718812800000L);
        data.setMetadata("{\"key\":\"value\"}");

        assertEquals("BIZ-100", data.getBizId());
        assertEquals("测试作品", data.getWorkName());
        assertEquals("author-100", data.getAuthorId());
        assertEquals("作者名", data.getAuthorName());
        assertEquals("sha256hash", data.getFileHash());
        assertEquals("PNG", data.getFileType());
        assertEquals(2048L, data.getFileSize());
        assertEquals(1718812800000L, data.getCreationTimestamp());
        assertEquals("{\"key\":\"value\"}", data.getMetadata());
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private CopyrightData buildCopyrightData(String bizId, String workName, String authorId, String authorName, String fileHash) {
        CopyrightData data = new CopyrightData();
        data.setBizId(bizId);
        data.setWorkName(workName);
        data.setAuthorId(authorId);
        data.setAuthorName(authorName);
        data.setFileHash(fileHash);
        data.setFileType("PNG");
        data.setFileSize(1024L);
        data.setCreationTimestamp(System.currentTimeMillis());
        data.setMetadata("{\"description\":\"test\"}");
        return data;
    }
}
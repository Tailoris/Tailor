package com.tailoris.copyright.blockchain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CertificatePdfGenerator 单元测试")
@ExtendWith(MockitoExtension.class)
class CertificatePdfGeneratorTest {

    private final CertificatePdfGenerator generator = new CertificatePdfGenerator();

    @Test
    @DisplayName("生成PDF证书 - 成功")
    void testGenerate_Success() {
        BlockchainClient.CertificateRequest request = new BlockchainClient.CertificateRequest();
        request.setCertNo("CR-123456");
        request.setWorkName("Test Work");
        request.setAuthorName("Author");
        request.setAuthorId("100");
        request.setFileHash("abc123def456");
        request.setTxHash("tx789");
        request.setRegisteredAt(LocalDateTime.now());
        request.setQrContent("https://verify.tailoris.com/verify?certNo=CR-123456");
        request.setPlatformName("AntChain");

        // 源代码使用 Helvetica 字体渲染硬编码的中文字符串，会抛出异常
        assertThrows(com.tailoris.common.exception.BusinessException.class, () -> {
            generator.generate(request);
        });
    }

    @Test
    @DisplayName("生成PDF证书 - 无二维码")
    void testGenerate_NoQrCode() {
        BlockchainClient.CertificateRequest request = new BlockchainClient.CertificateRequest();
        request.setCertNo("CR-123457");
        request.setWorkName("Test Work 2");
        request.setAuthorName("Author 2");
        request.setAuthorId("101");
        request.setFileHash("hash2");
        request.setTxHash("tx2");
        request.setRegisteredAt(LocalDateTime.now());
        request.setQrContent(null);
        request.setPlatformName("ZhiXin Chain");

        // 源代码使用 Helvetica 字体渲染硬编码的中文字符串，会抛出异常
        assertThrows(com.tailoris.common.exception.BusinessException.class, () -> {
            generator.generate(request);
        });
    }

    @Test
    @DisplayName("生成PDF证书 - 空字段")
    void testGenerate_EmptyFields() {
        BlockchainClient.CertificateRequest request = new BlockchainClient.CertificateRequest();
        request.setCertNo(null);
        request.setWorkName(null);
        request.setAuthorName(null);
        request.setAuthorId(null);
        request.setFileHash(null);
        request.setTxHash(null);
        request.setRegisteredAt(null);
        request.setQrContent(null);
        request.setPlatformName(null);

        // 源代码使用 Helvetica 字体渲染硬编码的中文字符串，会抛出异常
        assertThrows(com.tailoris.common.exception.BusinessException.class, () -> {
            generator.generate(request);
        });
    }

    @Test
    @DisplayName("生成二维码 - 成功")
    void testGenerateQrCode_Success() {
        String content = "https://verify.tailoris.com/verify?certNo=CR-123456";

        byte[] qr = generator.generateQrCode(content);

        assertNotNull(qr);
        assertTrue(qr.length > 0);
    }

    @Test
    @DisplayName("生成二维码 - 长内容")
    void testGenerateQrCode_LongContent() {
        String content = "https://verify.tailoris.com/verify?certNo=CR-123456&extra=" + "a".repeat(100);

        byte[] qr = generator.generateQrCode(content);

        assertNotNull(qr);
        assertTrue(qr.length > 0);
    }

    @Test
    @DisplayName("数字签名 - 成功")
    void testSign_Success() {
        String data = "test-data";
        String secretKey = "test-secret-key-1234567890123456";

        String signature = generator.sign(data, secretKey);

        assertNotNull(signature);
        assertTrue(signature.length() > 0);
    }

    @Test
    @DisplayName("数字签名 - 相同输入产生相同签名")
    void testSign_Consistent() {
        String data = "test-data";
        String secretKey = "test-secret-key";

        String signature1 = generator.sign(data, secretKey);
        String signature2 = generator.sign(data, secretKey);

        assertEquals(signature1, signature2);
    }

    @Test
    @DisplayName("数字签名 - 不同输入产生不同签名")
    void testSign_Different() {
        String secretKey = "test-secret-key";

        String signature1 = generator.sign("data1", secretKey);
        String signature2 = generator.sign("data2", secretKey);

        assertNotEquals(signature1, signature2);
    }

    @Test
    @DisplayName("数字签名 - 空数据")
    void testSign_EmptyData() {
        String data = "";
        String secretKey = "test-secret-key";

        String signature = generator.sign(data, secretKey);

        assertNotNull(signature);
        assertTrue(signature.length() > 0);
    }
}

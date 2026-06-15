package com.tailoris.product.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.oss.FileUploadService;
import com.tailoris.common.oss.UploadResult;
import com.tailoris.product.constant.ProductTypeConstants;
import com.tailoris.product.dto.CustomMeasurementRequest;
import com.tailoris.product.entity.CustomMeasurement;
import com.tailoris.product.entity.DigitalPattern;
import com.tailoris.product.entity.PatternDownloadToken;
import com.tailoris.product.entity.Product;
import com.tailoris.product.exception.ProductTypeMismatchException;
import com.tailoris.product.mapper.CustomMeasurementMapper;
import com.tailoris.product.mapper.DigitalPatternMapper;
import com.tailoris.product.mapper.PatternDownloadTokenMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductTypeServiceImpl 商品类型服务测试")
@ExtendWith(MockitoExtension.class)
class ProductTypeServiceImplTest {

    @Mock
    private DigitalPatternMapper digitalPatternMapper;

    @Mock
    private PatternDownloadTokenMapper downloadTokenMapper;

    @Mock
    private CustomMeasurementMapper customMeasurementMapper;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProductTypeServiceImpl productTypeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productTypeService, "maxDownloadCount", 3);
        ReflectionTestUtils.setField(productTypeService, "tokenTtlDays", 7);
    }

    @Test
    @DisplayName("创建实物商品扩展-无需扩展")
    void testCreateExtension_Physical() {
        Product product = new Product();
        product.setId(1L);
        product.setProductType(ProductTypeConstants.PHYSICAL);

        assertDoesNotThrow(() -> productTypeService.createExtension(product, null));
        verify(digitalPatternMapper, never()).insert(any(DigitalPattern.class));
        verify(customMeasurementMapper, never()).insert(any(CustomMeasurement.class));
    }

    @Test
    @DisplayName("创建数字纸样商品扩展-上传文件")
    void testCreateExtension_DigitalPattern() throws IOException {
        Product product = new Product();
        product.setId(1L);
        product.setProductType(ProductTypeConstants.DIGITAL_PATTERN);

        MockMultipartFile file = new MockMultipartFile(
                "file", "pattern.pdf", "application/pdf", "test content".getBytes());

        UploadResult uploadResult = new UploadResult();
        uploadResult.setObjectKey("pattern/123.pdf");
        uploadResult.setUrl("https://example.com/pattern/123.pdf");
        uploadResult.setSize(1024L);

        when(fileUploadService.upload(any(MultipartFile.class), eq("pattern"))).thenReturn(uploadResult);
        when(digitalPatternMapper.insert(any(DigitalPattern.class))).thenReturn(1);

        assertDoesNotThrow(() -> productTypeService.createExtension(product, file));
        verify(fileUploadService).upload(any(MultipartFile.class), eq("pattern"));
        verify(digitalPatternMapper).insert(any(DigitalPattern.class));
    }

    @Test
    @DisplayName("创建数字纸样商品扩展-文件为空")
    void testCreateExtension_DigitalPattern_EmptyFile() {
        Product product = new Product();
        product.setId(1L);
        product.setProductType(ProductTypeConstants.DIGITAL_PATTERN);

        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.createExtension(product, emptyFile));
        assertEquals("纸样文件不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("创建数字纸样商品扩展-上传失败")
    void testCreateExtension_DigitalPattern_UploadFailed() throws IOException {
        Product product = new Product();
        product.setId(1L);
        product.setProductType(ProductTypeConstants.DIGITAL_PATTERN);

        MockMultipartFile file = new MockMultipartFile(
                "file", "pattern.pdf", "application/pdf", "test content".getBytes());

        when(fileUploadService.upload(any(MultipartFile.class), eq("pattern")))
                .thenThrow(new IOException("Upload failed"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.createExtension(product, file));
        assertEquals("纸样文件上传失败", exception.getMessage());
    }

    @Test
    @DisplayName("创建定制商品扩展-无需文件")
    void testCreateExtension_Custom() {
        Product product = new Product();
        product.setId(1L);
        product.setProductType(ProductTypeConstants.CUSTOM);

        assertDoesNotThrow(() -> productTypeService.createExtension(product, null));
        verify(digitalPatternMapper, never()).insert(any(DigitalPattern.class));
        verify(customMeasurementMapper, never()).insert(any(CustomMeasurement.class));
    }

    @Test
    @DisplayName("创建商品扩展-商品类型不匹配")
    void testCreateExtension_InvalidType() {
        Product product = new Product();
        product.setId(1L);
        product.setProductType(99);

        assertThrows(ProductTypeMismatchException.class,
                () -> productTypeService.createExtension(product, null));
    }

    @Test
    @DisplayName("创建商品扩展-商品为空")
    void testCreateExtension_NullProduct() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.createExtension(null, null));
        assertEquals("商品或商品类型不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("删除商品扩展-数字纸样")
    void testDeleteExtension_DigitalPattern() {
        when(digitalPatternMapper.deleteByProductId(1L)).thenReturn(1);

        assertDoesNotThrow(() -> productTypeService.deleteExtension(1L, ProductTypeConstants.DIGITAL_PATTERN));
        verify(digitalPatternMapper).deleteByProductId(1L);
    }

    @Test
    @DisplayName("删除商品扩展-实物商品")
    void testDeleteExtension_Physical() {
        assertDoesNotThrow(() -> productTypeService.deleteExtension(1L, ProductTypeConstants.PHYSICAL));
        verify(digitalPatternMapper, never()).deleteByProductId(anyLong());
    }

    @Test
    @DisplayName("删除商品扩展-类型为空")
    void testDeleteExtension_NullType() {
        assertDoesNotThrow(() -> productTypeService.deleteExtension(1L, null));
        verify(digitalPatternMapper, never()).deleteByProductId(anyLong());
    }

    @Test
    @DisplayName("生成下载token成功")
    void testGenerateDownloadToken_Success() {
        DigitalPattern pattern = new DigitalPattern();
        pattern.setId(1L);
        pattern.setProductId(100L);
        pattern.setFileKey("pattern/123.pdf");
        pattern.setLicenseDurationDays(30);

        when(digitalPatternMapper.selectById(1L)).thenReturn(pattern);
        when(downloadTokenMapper.insert(any(PatternDownloadToken.class))).thenAnswer(invocation -> {
            PatternDownloadToken token = invocation.getArgument(0);
            token.setId(1L);
            return 1;
        });
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        PatternDownloadToken result = productTypeService.generateDownloadToken(1L, 1000L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(1000L, result.getOrderId());
        assertEquals(1L, result.getPatternId());
        assertEquals(100L, result.getProductId());
        assertNotNull(result.getToken());
        assertEquals(3, result.getMaxDownloadCount());
        assertEquals(0, result.getUsedCount());
        assertNotNull(result.getExpireTime());
    }

    @Test
    @DisplayName("生成下载token-纸样不存在")
    void testGenerateDownloadToken_PatternNotFound() {
        when(digitalPatternMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.generateDownloadToken(1L, 1000L, 999L));
        assertEquals("数字纸样不存在", exception.getMessage());
    }

    @Test
    @DisplayName("下载纸样成功")
    void testDownloadByToken_Success() {
        PatternDownloadToken token = new PatternDownloadToken();
        token.setId(1L);
        token.setUserId(1L);
        token.setPatternId(1L);
        token.setOrderId(1000L);
        token.setToken("valid-token");
        token.setMaxDownloadCount(3);
        token.setUsedCount(0);
        token.setExpireTime(LocalDateTime.now().plusDays(7));

        DigitalPattern pattern = new DigitalPattern();
        pattern.setId(1L);
        pattern.setFileKey("pattern/123.pdf");

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(downloadTokenMapper.selectByToken("valid-token")).thenReturn(token);
        when(downloadTokenMapper.atomicConsume(1L)).thenReturn(1);
        when(digitalPatternMapper.selectById(1L)).thenReturn(pattern);
        when(fileUploadService.generatePresignedUrl("pattern/123.pdf", 600))
                .thenReturn("https://example.com/presigned-url");
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any());

        String result = productTypeService.downloadByToken("valid-token", "192.168.1.1");

        assertNotNull(result);
        assertEquals("https://example.com/presigned-url", result);
        verify(downloadTokenMapper).atomicConsume(1L);
        verify(digitalPatternMapper).incrementDownloadCount(1L);
    }

    @Test
    @DisplayName("下载纸样-token为空")
    void testDownloadByToken_EmptyToken() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.downloadByToken("", "192.168.1.1"));
        assertEquals("下载token不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("下载纸样-token已使用")
    void testDownloadByToken_TokenUsed() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.downloadByToken("used-token", "192.168.1.1"));
        assertEquals("下载链接已失效，请重新获取", exception.getMessage());
    }

    @Test
    @DisplayName("下载纸样-token不存在")
    void testDownloadByToken_TokenNotFound() {
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(downloadTokenMapper.selectByToken("invalid-token")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.downloadByToken("invalid-token", "192.168.1.1"));
        assertEquals("下载token不存在或已过期", exception.getMessage());
    }

    @Test
    @DisplayName("下载纸样-token已过期")
    void testDownloadByToken_TokenExpired() {
        PatternDownloadToken token = new PatternDownloadToken();
        token.setId(1L);
        token.setToken("expired-token");
        token.setExpireTime(LocalDateTime.now().minusDays(1));

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(downloadTokenMapper.selectByToken("expired-token")).thenReturn(token);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.downloadByToken("expired-token", "192.168.1.1"));
        assertEquals("下载token已过期", exception.getMessage());
    }

    @Test
    @DisplayName("下载纸样-达到最大下载次数")
    void testDownloadByToken_MaxDownloadReached() {
        PatternDownloadToken token = new PatternDownloadToken();
        token.setId(1L);
        token.setToken("max-token");
        token.setMaxDownloadCount(3);
        token.setUsedCount(3);
        token.setExpireTime(LocalDateTime.now().plusDays(7));

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(downloadTokenMapper.selectByToken("max-token")).thenReturn(token);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.downloadByToken("max-token", "192.168.1.1"));
        assertEquals("已达最大下载次数限制", exception.getMessage());
    }

    @Test
    @DisplayName("下载纸样-并发冲突")
    void testDownloadByToken_ConcurrentConflict() {
        PatternDownloadToken token = new PatternDownloadToken();
        token.setId(1L);
        token.setToken("concurrent-token");
        token.setExpireTime(LocalDateTime.now().plusDays(7));
        token.setMaxDownloadCount(3);
        token.setUsedCount(0);

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(downloadTokenMapper.selectByToken("concurrent-token")).thenReturn(token);
        when(downloadTokenMapper.atomicConsume(1L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.downloadByToken("concurrent-token", "192.168.1.1"));
        assertEquals("下载链接被其他请求占用，请重试", exception.getMessage());
    }

    @Test
    @DisplayName("查询用户拥有的纸样")
    void testListOwnedPatterns() {
        DigitalPattern pattern = new DigitalPattern();
        pattern.setId(1L);
        pattern.setProductId(100L);

        when(digitalPatternMapper.listOwnedByUser(1L)).thenReturn(Arrays.asList(pattern));

        List<DigitalPattern> result = productTypeService.listOwnedPatterns(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(digitalPatternMapper).listOwnedByUser(1L);
    }

    @Test
    @DisplayName("保存定制参数成功")
    void testSaveMeasurement_Success() {
        CustomMeasurementRequest request = new CustomMeasurementRequest();
        request.setHeight(new BigDecimal("175"));
        request.setWeight(new BigDecimal("70"));
        request.setBust(new BigDecimal("90"));
        request.setWaist(new BigDecimal("75"));
        request.setHip(new BigDecimal("95"));

        when(customMeasurementMapper.insert(any(CustomMeasurement.class))).thenAnswer(invocation -> {
            CustomMeasurement m = invocation.getArgument(0);
            m.setId(1L);
            return 1;
        });

        CustomMeasurement result = productTypeService.saveMeasurement(1L, 1000L, 100L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(1000L, result.getOrderId());
        assertEquals(100L, result.getProductId());
        assertEquals(new BigDecimal("175"), result.getHeight());
        assertEquals(new BigDecimal("70"), result.getWeight());
        assertEquals(new BigDecimal("90"), result.getBust());
        assertEquals(new BigDecimal("75"), result.getWaist());
        assertEquals(new BigDecimal("95"), result.getHip());
        verify(customMeasurementMapper).insert(any(CustomMeasurement.class));
    }

    @Test
    @DisplayName("保存定制参数-参数为空")
    void testSaveMeasurement_NullParams() {
        CustomMeasurementRequest request = new CustomMeasurementRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.saveMeasurement(null, 1000L, 100L, request));
        assertEquals("用户/订单/商品ID不能为空", exception.getMessage());
    }

    @Test
    @DisplayName("保存定制参数-身高异常")
    void testSaveMeasurement_InvalidHeight() {
        CustomMeasurementRequest request = new CustomMeasurementRequest();
        request.setHeight(new BigDecimal("50"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.saveMeasurement(1L, 1000L, 100L, request));
        assertTrue(exception.getMessage().contains("身高数据异常"));
    }

    @Test
    @DisplayName("保存定制参数-胸围异常")
    void testSaveMeasurement_InvalidBust() {
        CustomMeasurementRequest request = new CustomMeasurementRequest();
        request.setBust(new BigDecimal("30"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.saveMeasurement(1L, 1000L, 100L, request));
        assertTrue(exception.getMessage().contains("胸围数据异常"));
    }

    @Test
    @DisplayName("保存定制参数-腰围异常")
    void testSaveMeasurement_InvalidWaist() {
        CustomMeasurementRequest request = new CustomMeasurementRequest();
        request.setWaist(new BigDecimal("200"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productTypeService.saveMeasurement(1L, 1000L, 100L, request));
        assertTrue(exception.getMessage().contains("腰围数据异常"));
    }

    @Test
    @DisplayName("根据订单查询定制参数")
    void testGetMeasurementByOrder() {
        CustomMeasurement measurement = new CustomMeasurement();
        measurement.setId(1L);
        measurement.setOrderId(1000L);

        when(customMeasurementMapper.selectByOrderId(1000L)).thenReturn(measurement);

        CustomMeasurement result = productTypeService.getMeasurementByOrder(1000L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1000L, result.getOrderId());
        verify(customMeasurementMapper).selectByOrderId(1000L);
    }
}

package com.tailoris.product.service;

import com.tailoris.product.dto.CustomMeasurementRequest;
import com.tailoris.product.entity.CustomMeasurement;
import com.tailoris.product.entity.DigitalPattern;
import com.tailoris.product.entity.PatternDownloadToken;
import com.tailoris.product.entity.Product;
import com.tailoris.product.exception.ProductTypeMismatchException;

import java.util.List;

/**
 * 商品类型差异化服务接口 - PRD-008.
 *
 * <p>针对三种商品类型提供差异化处理能力：</p>
 * <ul>
 *   <li>实物：标准SKU + 库存 + 物流</li>
 *   <li>数字纸样：文件下载 + 授权 + 版权保护</li>
 *   <li>定制：身材采集 + 工艺选择 + 生产排期</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface ProductTypeService {

    /**
     * 创建商品类型扩展信息.
     */
    void createExtension(Product product, Object extensionData);

    /**
     * 删除商品类型扩展.
     */
    void deleteExtension(Long productId, Integer productType);

    /**
     * 数字纸样：生成下载凭证（购买后调用）.
     */
    PatternDownloadToken generateDownloadToken(Long userId, Long orderId, Long patternId);

    /**
     * 数字纸样：使用token下载.
     */
    String downloadByToken(String token, String clientIp);

    /**
     * 数字纸样：查询用户已购纸样列表.
     */
    List<DigitalPattern> listOwnedPatterns(Long userId);

    /**
     * 定制商品：保存定制参数.
     */
    CustomMeasurement saveMeasurement(Long userId, Long orderId, Long productId,
                                       CustomMeasurementRequest request);

    /**
     * 定制商品：获取订单定制参数.
     */
    CustomMeasurement getMeasurementByOrder(Long orderId);
}

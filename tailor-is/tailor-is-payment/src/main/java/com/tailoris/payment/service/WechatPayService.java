package com.tailoris.payment.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 微信支付服务接口.
 * <p>支持微信支付 V3 API，提供 Native 支付（扫码支付）、JSAPI 支付（公众号/小程序内支付）、
 * 订单查询、关闭订单、退款、退款查询等操作。</p>
 */
public interface WechatPayService {

    /**
     * 创建支付订单（Native 扫码支付）.
     *
     * @param orderNo   商户订单号
     * @param amount    订单金额（元）
     * @param body      商品描述
     * @param clientIp  客户端 IP
     * @param notifyUrl 回调通知地址
     * @return 包含 code_url（二维码链接）等信息的 Map
     */
    Map<String, Object> createPayment(String orderNo, BigDecimal amount, String body,
                                       String clientIp, String notifyUrl);

    /**
     * 创建支付订单（JSAPI 公众号/小程序支付）.
     *
     * @param orderNo   商户订单号
     * @param amount    订单金额（元）
     * @param openId    用户 OpenID
     * @param body      商品描述
     * @param clientIp  客户端 IP
     * @param notifyUrl 回调通知地址
     * @return 包含 prepay_id、paySign 等信息的 Map
     */
    Map<String, Object> createJsapiPayment(String orderNo, BigDecimal amount, String openId,
                                            String body, String clientIp, String notifyUrl);

    /**
     * 查询支付订单.
     *
     * @param transactionId 微信支付交易号（与 outTradeNo 二选一）
     * @param outTradeNo    商户订单号
     * @return 订单状态信息
     */
    Map<String, Object> queryPayment(String transactionId, String outTradeNo);

    /**
     * 关闭支付订单.
     *
     * @param outTradeNo 商户订单号
     * @return 是否成功
     */
    boolean closePayment(String outTradeNo);

    /**
     * 创建退款.
     *
     * @param outTradeNo   商户订单号
     * @param outRefundNo  商户退款单号
     * @param refundAmount 退款金额（元）
     * @param totalAmount  原订单金额（元）
     * @param reason       退款原因
     * @return 退款信息
     */
    Map<String, Object> createRefund(String outTradeNo, String outRefundNo,
                                      BigDecimal refundAmount, BigDecimal totalAmount, String reason);

    /**
     * 查询退款.
     *
     * @param outRefundNo 商户退款单号
     * @return 退款状态信息
     */
    Map<String, Object> queryRefund(String outRefundNo);

    /**
     * 验证回调通知签名.
     *
     * @param body      回调请求体
     * @param signature 签名头
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param serialNo  证书序列号
     * @return 是否验证通过
     */
    boolean verifyCallback(String body, String signature, String timestamp, String nonce, String serialNo);

    /**
     * 解密回调敏感数据.
     *
     * @param ciphertext 密文
     * @param nonce      AEAD 随机数
     * @param associatedData 附加数据
     * @return 解密后的明文
     */
    String decryptCallbackData(String ciphertext, String nonce, String associatedData);
}
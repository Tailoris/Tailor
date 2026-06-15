/**
 * Feign Client 接口存放规范。
 *
 * <p>本包用于存放模块间服务调用的 Feign 声明式客户端接口。
 * Tailor IS 平台采用微服务架构，各业务模块通过 Feign 进行同步服务间调用，
 * 并通过 RabbitMQ 消息队列进行异步解耦通信。</p>
 *
 * <h3>命名规范</h3>
 * <ul>
 *   <li>接口命名：{目标服务名}Client，如 {@code OrderClient}、{@code PaymentClient}</li>
 *   <li>包路径：{@code com.tailoris.common.client.{domain}}</li>
 *   <li>降级实现：{@code {服务名}ClientFallback}，放在 {@code .fallback} 子包中</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @FeignClient(name = "tailor-is-order", path = "/order", fallback = OrderClientFallback.class)
 * public interface OrderClient {
 *     @GetMapping("/detail/{orderNo}")
 *     Result<OrderInfo> getOrderDetail(@PathVariable String orderNo);
 * }
 * }</pre>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>每个 Feign 客户端必须提供降级实现（Sentinel 熔断）</li>
 *   <li>跨模块写操作优先考虑消息队列异步解耦</li>
 *   <li>读操作可使用 Feign 同步调用，但需设置合理的超时和重试策略</li>
 *   <li>敏感数据传输需加密，日志输出需脱敏</li>
 *   <li>建议将 Feign 接口定义在 tailor-is-common 模块中，供各模块共享</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
package com.tailoris.common.client;
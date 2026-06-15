package com.tailoris.common.util;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * N+1查询修复工具 - 修复 B-H18/TD-04
 *
 * <p>提供批量查询工具，避免循环中单条查询导致的N+1问题。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 修复前（N+1）
 * List<OrderInfo> orders = orderMapper.selectBatchIds(orderIds);
 * for (OrderInfo order : orders) {
 *     User user = userMapper.selectById(order.getUserId());  // N次查询
 *     order.setUser(user);
 * }
 *
 * // 修复后（1次查询）
 * List<OrderInfo> orders = orderMapper.selectBatchIds(orderIds);
 * Set<Long> userIds = orders.stream().map(OrderInfo::getUserId).collect(Collectors.toSet());
 * Map<Long, User> userMap = BatchQueryUtil.batchGet(userMapper, userIds, User::getId);
 * orders.forEach(o -> o.setUser(userMap.get(o.getUserId())));
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
public class BatchQueryUtil {

    /**
     * 批量获取实体（解决N+1问题）
     *
     * @param service    MyBatis-Plus Service
     * @param ids        ID集合
     * @param idFunction 实体ID的getter
     * @param <T>        实体类型
     * @param <ID>       ID类型
     * @return ID -> 实体 的映射
     */
    public static <T, ID extends Serializable> Map<ID, T> batchGet(IService<T> service, Collection<ID> ids, Function<T, ID> idFunction) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<T> entities = service.listByIds(ids);
            return entities.stream().collect(Collectors.toMap(idFunction, e -> e, (a, b) -> a));
        } catch (Exception e) {
            log.error("批量查询失败, ids.size={}", ids.size(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量查询并赋值
     *
     * @param sourceList     源列表
     * @param idExtractor    从源列表元素提取ID
     * @param service        目标Service
     * @param idFunction     目标实体的ID getter
     * @param targetSetter   目标实体的setter
     * @param <S>            源类型
     * @param <T>            目标类型
     * @param <ID>           ID类型
     */
    public static <S, T, ID extends Serializable> void batchAssign(
            List<S> sourceList,
            Function<S, ID> idExtractor,
            IService<T> service,
            Function<T, ID> idFunction,
            java.util.function.BiConsumer<S, T> targetSetter) {
        if (sourceList == null || sourceList.isEmpty()) {
            return;
        }
        Set<ID> ids = sourceList.stream()
                .map(idExtractor)
                .collect(Collectors.toSet());
        Map<ID, T> targetMap = batchGet(service, ids, idFunction);
        sourceList.forEach(s -> targetSetter.accept(s, targetMap.get(idExtractor.apply(s))));
    }

    /**
     * 分页批量查询
     *
     * @param service  Service
     * @param pageNum  页码
     * @param pageSize 页大小
     * @param wrapper  查询条件
     * @param <T>      实体类型
     * @return 分页结果
     */
    public static <T> IPage<T> batchPage(IService<T> service, int pageNum, int pageSize, Wrapper<T> wrapper) {
        Page<T> page = new Page<>(pageNum, pageSize);
        return service.page(page, wrapper);
    }
}

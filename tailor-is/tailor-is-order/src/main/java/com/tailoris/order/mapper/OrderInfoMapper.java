package com.tailoris.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderItem;
import com.tailoris.order.entity.OrderLogistics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 查询订单详情（含订单项和物流信息），使用三表 JOIN 一次性查询，消除 N+1 问题。
     * <p>XML 映射定义在 OrderInfoMapper.xml 的 OrderDetailResultMap 中，
     * 通过列别名前缀 (oi_ / oit_ / ol_) 消歧同名字段。</p>
     */
    OrderInfo selectOrderDetailWithItems(@Param("orderNo") String orderNo);

    /**
     * 批量查询订单项，用于订单列表页批量填充，避免 N+1 查询。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单项列表（按 orderId 分组后填充到各 OrderInfo.orderItems）
     */
    List<OrderItem> selectOrderItemsByIds(@Param("orderIds") List<Long> orderIds);

    /**
     * 批量查询物流信息，用于订单列表页批量填充。
     *
     * @param orderIds 订单 ID 列表
     * @return 物流信息列表
     */
    List<OrderLogistics> selectLogisticsByIds(@Param("orderIds") List<Long> orderIds);
}
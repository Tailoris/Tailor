package com.tailoris.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.dto.AfterSaleRequest;
import com.tailoris.order.dto.TicketProcessRequest;
import com.tailoris.order.entity.AfterSaleTicket;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderItem;
import com.tailoris.order.mapper.AfterSaleTicketMapper;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderItemMapper;
import com.tailoris.order.service.AfterSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后处理服务实现
 *
 * <p>负责处理售后工单的创建、审核、退款和关闭等核心业务流程。
 * 支持仅退款和退货退款两种售后类型，与PaymentService联动处理退款。}</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private final AfterSaleTicketMapper afterSaleTicketMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTicket(Long userId, AfterSaleRequest request) {
        OrderInfo order = orderInfoMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        if (order.getStatus() == OrderConstants.ORDER_STATUS_CANCELLED) {
            throw new BusinessException("订单已取消，无法申请售后");
        }

        String ticketNo = generateTicketNo();

        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setTicketNo(ticketNo);
        ticket.setOrderId(order.getId());
        ticket.setOrderNo(order.getOrderNo());
        ticket.setOrderItemId(request.getOrderItemId());
        ticket.setUserId(userId);
        ticket.setMerchantId(order.getMerchantId());
        ticket.setShopId(order.getShopId());
        ticket.setProductId(0L);
        ticket.setSkuId(0L);
        ticket.setTicketType(request.getTicketType());
        ticket.setStatus(OrderConstants.TICKET_STATUS_PENDING);
        ticket.setReason(request.getReason());
        ticket.setDescription(request.getDescription());
        ticket.setRefundAmount(request.getRefundAmount());
        ticket.setRefundQuantity(request.getRefundQuantity() != null ? request.getRefundQuantity() : 1);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                ticket.setImages(objectMapper.writeValueAsString(request.getImages()));
            } catch (JsonProcessingException e) {
                log.warn("售后凭证图片序列化失败", e);
            }
        }
        if (StringUtils.hasText(request.getVideoUrl())) {
            ticket.setVideoUrl(request.getVideoUrl());
        }

        afterSaleTicketMapper.insert(ticket);

        order.setStatus(OrderConstants.ORDER_STATUS_REFUNDING);
        orderInfoMapper.updateById(order);

        log.info("售后工单创建成功, userId: {}, ticketNo: {}, refundAmount: {}", userId, ticketNo, request.getRefundAmount());
        return ticketNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long userId, TicketProcessRequest request) {
        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(request.getTicketId());
        if (ticket == null) {
            throw new BusinessException("售后工单不存在");
        }

        if (request.getProcessResult() == 1) {
            ticket.setStatus(OrderConstants.TICKET_STATUS_COMPLETED);
            ticket.setMerchantRemark(request.getProcessRemark());
            ticket.setMerchantHandleTime(LocalDateTime.now());
            ticket.setProcessedAt(LocalDateTime.now());
        } else if (request.getProcessResult() == 0) {
            ticket.setStatus(OrderConstants.TICKET_STATUS_REJECTED);
            ticket.setMerchantRemark(request.getProcessRemark());
            ticket.setMerchantHandleTime(LocalDateTime.now());
        }

        afterSaleTicketMapper.updateById(ticket);

        log.info("售后工单处理完成, ticketId: {}, result: {}", request.getTicketId(), request.getProcessResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long userId, Long ticketId, String remark) {
        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException("售后工单不存在");
        }
        if (!ticket.getMerchantId().equals(userId) && !ticket.getShopId().equals(userId)) {
            throw new BusinessException("无权操作该售后工单");
        }
        if (!Integer.valueOf(OrderConstants.TICKET_STATUS_PENDING).equals(ticket.getStatus())
                && !Integer.valueOf(OrderConstants.TICKET_STATUS_PROCESSING).equals(ticket.getStatus())) {
            throw new BusinessException("售后工单状态异常");
        }

        ticket.setStatus(OrderConstants.TICKET_STATUS_COMPLETED);
        ticket.setMerchantRemark(remark);
        ticket.setMerchantHandleTime(LocalDateTime.now());
        ticket.setProcessedAt(LocalDateTime.now());
        afterSaleTicketMapper.updateById(ticket);

        OrderInfo order = orderInfoMapper.selectById(ticket.getOrderId());
        if (order != null) {
            order.setStatus(OrderConstants.ORDER_STATUS_REFUNDED);
            orderInfoMapper.updateById(order);
        }

        log.info("退款审批通过, ticketId: {}, refundAmount: {}", ticketId, ticket.getRefundAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTicket(Long userId, Long ticketId, String remark) {
        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException("售后工单不存在");
        }
        if (!ticket.getMerchantId().equals(userId) && !ticket.getShopId().equals(userId)) {
            throw new BusinessException("无权操作该售后工单");
        }
        if (!Integer.valueOf(OrderConstants.TICKET_STATUS_PENDING).equals(ticket.getStatus())
                && !Integer.valueOf(OrderConstants.TICKET_STATUS_PROCESSING).equals(ticket.getStatus())) {
            throw new BusinessException("售后工单状态异常");
        }

        ticket.setStatus(OrderConstants.TICKET_STATUS_REJECTED);
        ticket.setMerchantRemark(remark);
        ticket.setMerchantHandleTime(LocalDateTime.now());
        afterSaleTicketMapper.updateById(ticket);

        OrderInfo order = orderInfoMapper.selectById(ticket.getOrderId());
        if (order != null && order.getStatus() == OrderConstants.ORDER_STATUS_REFUNDING) {
            order.setStatus(OrderConstants.ORDER_STATUS_COMPLETED);
            orderInfoMapper.updateById(order);
        }

        log.info("售后工单已拒绝, ticketId: {}, reason: {}", ticketId, remark);
    }

    @Override
    public Page<AfterSaleTicket> listTicketsByUser(Long userId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<AfterSaleTicket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AfterSaleTicket::getUserId, userId);

        if (status != null) {
            queryWrapper.eq(AfterSaleTicket::getStatus, status);
        }

        queryWrapper.orderByDesc(AfterSaleTicket::getCreateTime);

        Page<AfterSaleTicket> page = new Page<>(pageNum, pageSize);
        return afterSaleTicketMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Page<AfterSaleTicket> listTicketsByMerchant(Long merchantId, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<AfterSaleTicket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AfterSaleTicket::getMerchantId, merchantId);

        if (status != null) {
            queryWrapper.eq(AfterSaleTicket::getStatus, status);
        }

        queryWrapper.orderByDesc(AfterSaleTicket::getCreateTime);

        Page<AfterSaleTicket> page = new Page<>(pageNum, pageSize);
        return afterSaleTicketMapper.selectPage(page, queryWrapper);
    }

    @Override
    public AfterSaleTicket getTicketDetail(String ticketNo) {
        LambdaQueryWrapper<AfterSaleTicket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AfterSaleTicket::getTicketNo, ticketNo);
        AfterSaleTicket ticket = afterSaleTicketMapper.selectOne(queryWrapper);

        if (ticket == null) {
            throw new BusinessException("售后工单不存在");
        }

        return ticket;
    }

    private String generateTicketNo() {
        long id = SnowflakeIdGenerator.getInstance().nextId();
        return OrderConstants.TICKET_NO_PREFIX + id;
    }
}

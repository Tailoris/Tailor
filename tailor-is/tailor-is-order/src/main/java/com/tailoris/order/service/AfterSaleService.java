package com.tailoris.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.order.dto.AfterSaleRequest;
import com.tailoris.order.dto.TicketProcessRequest;
import com.tailoris.order.entity.AfterSaleTicket;

public interface AfterSaleService {

    String createTicket(Long userId, AfterSaleRequest request);

    void processTicket(Long userId, TicketProcessRequest request);

    void approveRefund(Long userId, Long ticketId, String remark);

    void rejectTicket(Long userId, Long ticketId, String remark);

    Page<AfterSaleTicket> listTicketsByUser(Long userId, Integer status, int pageNum, int pageSize);

    Page<AfterSaleTicket> listTicketsByMerchant(Long merchantId, Integer status, int pageNum, int pageSize);

    AfterSaleTicket getTicketDetail(String ticketNo);
}

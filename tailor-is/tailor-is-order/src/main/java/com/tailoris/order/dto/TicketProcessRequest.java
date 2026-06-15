package com.tailoris.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketProcessRequest {

    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    @NotNull(message = "处理结果不能为空")
    private Integer processResult;

    private String processRemark;
}

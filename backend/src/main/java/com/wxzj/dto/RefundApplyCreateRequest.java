package com.wxzj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundApplyCreateRequest {

    @NotNull(message = "请选择资金账户")
    private Long accountId;

    /** TRANSFER / DEMOLITION / OVERPAY */
    @NotNull(message = "退款原因不能为空")
    private String reason;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal amount;

    private String remark;
}

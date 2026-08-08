package com.wxzj.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositCreateRequest {

    @NotNull(message = "请选择房屋")
    private Long houseId;

    private Long standardId;

    /** INITIAL / RENEWAL / SUPPLEMENT */
    private String type = "INITIAL";

    /** 可选，默认取标准单价 */
    private BigDecimal unitPrice;

    private String remark;
}

package com.wxzj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UseApplyCreateRequest {

    @NotNull(message = "请选择小区")
    private Long communityId;

    @NotBlank(message = "维修项目名称不能为空")
    private String title;

    private String reason;

    private String itemDesc;

    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    @NotNull(message = "请选择涉及房屋")
    private List<Long> houseIds;

    private String remark;
}

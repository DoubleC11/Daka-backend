package com.daka.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BillDto {

    /** type 为空时按分类自身的 type 推断 */
    public record SaveReq(
            @NotNull(message = "请选择分类") Long categoryId,
            Integer type,
            @NotNull(message = "请填写金额")
            @DecimalMin(value = "0.01", message = "金额需大于 0")
            BigDecimal amount,
            LocalDate billDate,
            @NotBlank(message = "请填写事项")
            String title,
            String note
    ) {
    }
}

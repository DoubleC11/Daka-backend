package com.daka.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GoalDto {

    public record SaveReq(
            @NotBlank(message = "目标名称不能为空") String title,
            BigDecimal targetValue,
            BigDecimal currentValue,
            String unit,
            String color,
            LocalDate deadline,
            Integer status
    ) {
    }
}

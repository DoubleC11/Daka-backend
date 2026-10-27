package com.daka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryDto {

    /** type: 1收入 2支出 */
    public record SaveReq(
            @NotBlank(message = "分类名称不能为空") String name,
            @NotNull(message = "请选择收入或支出") Integer type,
            String color,
            @NotBlank(message = "请填写图标文字") String iconText
    ) {
    }
}

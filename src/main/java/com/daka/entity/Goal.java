package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("goal")
public class Goal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private BigDecimal targetValue;

    private BigDecimal currentValue;

    private String unit;

    private String color;

    private LocalDate deadline;

    /** 1进行中 2已完成 3已放弃 */
    private Integer status;

    private LocalDateTime createTime;
}

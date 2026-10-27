package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("habit")
public class Habit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** 主题色标识：violet/sky/emerald/amber/pink/rose/teal/orange */
    private String color;

    private String iconText;

    private String frequency;

    private Integer sortOrder;

    private Integer archived;

    private LocalDateTime createTime;

    // ---- 非数据库字段，查询时补充 ----
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer streak;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<String> weekDots;
}

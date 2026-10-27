package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("bill")
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long categoryId;

    /** 1收入 2支出 */
    private Integer type;

    private BigDecimal amount;

    private LocalDate billDate;

    private String title;

    private String note;

    private LocalDateTime createTime;

    // ---- 非数据库字段，查询时联表补充 ----
    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String categoryColor;

    @TableField(exist = false)
    private String categoryIcon;
}

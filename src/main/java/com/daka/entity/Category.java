package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** null 表示系统预置分类，全员可见 */
    private Long userId;

    private String name;

    /** 1收入 2支出 */
    private Integer type;

    private String color;

    private String iconText;

    private Integer sortOrder;
}

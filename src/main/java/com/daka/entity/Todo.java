package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("todo")
public class Todo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long goalId;

    private String title;

    /** 生活 / 工作 / 读书 */
    private String tag;

    private LocalDate dueDate;

    private Integer done;

    private LocalDateTime doneTime;

    private Integer sortOrder;

    private LocalDateTime createTime;
}

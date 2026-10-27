package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("habit_record")
public class HabitRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long habitId;

    private LocalDate recordDate;

    private Integer done;

    private String note;

    private LocalDateTime createTime;
}

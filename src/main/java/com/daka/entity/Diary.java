package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("diary")
public class Diary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate diaryDate;

    /** 开心 / 平静 / 疲惫 / 低落 */
    private String mood;

    private String content;

    /** 逗号分隔标签 */
    private String tags;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

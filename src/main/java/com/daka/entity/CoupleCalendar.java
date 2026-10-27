package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情侣共同日历：一天一条记录，双方都能编辑。
 * <p>
 * userAId / userBId 固定存「较小 id / 较大 id」，同一对用户共用一份日历，
 * 解绑后重新绑定同一个人也能找回以前写的内容。
 */
@Data
@TableName("couple_calendar")
public class CoupleCalendar {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配对中较小的用户 id */
    private Long userAId;

    /** 配对中较大的用户 id */
    private Long userBId;

    /** 哪天 */
    private LocalDate eventDate;

    /** 这天干了什么 */
    private String content;

    /** 最后编辑人 */
    private Long editorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

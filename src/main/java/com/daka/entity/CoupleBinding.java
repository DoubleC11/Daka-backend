package com.daka.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 情侣绑定关系。
 * status：1 待接受（已生成邀请码）2 已绑定 3 已解除
 */
@Data
@TableName("couple_binding")
public class CoupleBinding {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起方 */
    private Long userAId;

    /** 接受方，待接受时为 null */
    private Long userBId;

    private Integer status;

    private String inviteCode;

    /** 由数据库默认值填充 */
    private LocalDateTime createTime;

    private LocalDateTime bindTime;
}

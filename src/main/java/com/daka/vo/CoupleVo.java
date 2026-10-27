package com.daka.vo;

import java.util.List;

public class CoupleVo {

    /**
     * 邀请码。
     * expireMinutes：剩余有效分钟数（向上取整，最小 1）
     * expireSeconds：剩余有效秒数（接口返回那一刻的快照）
     * expireAt：失效时刻（毫秒时间戳），前端据此本地每秒递减，刷新页面也能算准
     */
    public record Invite(String code, int expireMinutes, int expireSeconds, long expireAt) {
    }

    /** 对方概要 */
    public record Partner(Long id, String nickname, String username, String avatarColor) {
    }

    /** 绑定信息，未绑定时接口 data 为 null */
    public record Info(Long bindingId, Partner partner, String bindTime, int days) {
    }

    /**
     * 共同日历的一天。
     * date：yyyy-MM-dd；editorName：最后编辑人；updatedAt：yyyy-MM-dd HH:mm
     */
    public record CalendarDay(String date, String content, Long editorId, String editorName, String updatedAt) {
    }

    /** 某个月的日历：month=yyyy-MM，days=本月有记录的天数 */
    public record CalendarMonth(String month, int days, List<CalendarDay> records) {
    }
}

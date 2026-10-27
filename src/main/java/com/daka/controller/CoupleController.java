package com.daka.controller;

import com.daka.common.Result;
import com.daka.dto.CoupleDto;
import com.daka.service.CoupleCalendarService;
import com.daka.service.CoupleService;
import com.daka.vo.CoupleVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 情侣空间：绑定关系维护 + 共同日历。
 * 查看对方业务数据走各业务接口的 viewUserId 参数。
 */
@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    private final CoupleService coupleService;
    private final CoupleCalendarService calendarService;

    public CoupleController(CoupleService coupleService, CoupleCalendarService calendarService) {
        this.coupleService = coupleService;
        this.calendarService = calendarService;
    }

    /** 当前绑定信息；未绑定时 data 为 null */
    @GetMapping
    public Result<CoupleVo.Info> info() {
        return Result.ok(coupleService.info());
    }

    /** 查看当前有效的邀请码（不生成新码）；没有则 data 为 null */
    @GetMapping("/invite")
    public Result<CoupleVo.Invite> currentInvite(HttpServletResponse response) {
        // 邀请码是随时可变的状态，禁止浏览器缓存，避免不同账号读到同一份响应
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        return Result.ok(coupleService.currentInvite());
    }

    /**
     * 生成邀请码。
     * refresh=true 表示“重新生成”：作废旧码并返回新码。
     */
    @PostMapping("/invite")
    public Result<CoupleVo.Invite> invite(@RequestParam(defaultValue = "false") boolean refresh) {
        return Result.ok(coupleService.invite(refresh));
    }

    /** 用对方的邀请码完成绑定 */
    @PostMapping("/bind")
    public Result<CoupleVo.Info> bind(@Valid @RequestBody CoupleDto.BindReq req) {
        return Result.ok(coupleService.bind(req.code()));
    }

    /** 解除绑定 */
    @DeleteMapping
    public Result<Void> unbind() {
        coupleService.unbind();
        return Result.ok();
    }

    /**
     * 共同日历：某个月的记录（month=yyyy-MM，缺省当月）。
     * 双方看到的永远是同一份数据。
     */
    @GetMapping("/calendar")
    public Result<CoupleVo.CalendarMonth> calendar(@RequestParam(required = false) String month) {
        return Result.ok(calendarService.month(month));
    }

    /** 写/改某一天；content 为空表示清空这一天，data 返回 null */
    @PutMapping("/calendar")
    public Result<CoupleVo.CalendarDay> saveCalendar(@Valid @RequestBody CoupleDto.CalendarSaveReq req) {
        return Result.ok(calendarService.save(req));
    }

    /** 删掉某一天的记录 */
    @DeleteMapping("/calendar")
    public Result<Void> deleteCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        calendarService.delete(date);
        return Result.ok();
    }
}

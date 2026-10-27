package com.daka.controller;

import com.daka.common.Result;
import com.daka.dto.HabitDto;
import com.daka.entity.Habit;
import com.daka.service.HabitService;
import com.daka.vo.HabitVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping
    public Result<List<HabitVo.Item>> list() {
        return Result.ok(habitService.list());
    }

    @PostMapping
    public Result<Habit> create(@Valid @RequestBody HabitDto.SaveReq req) {
        return Result.ok(habitService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Habit> update(@PathVariable Long id, @Valid @RequestBody HabitDto.SaveReq req) {
        return Result.ok(habitService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        habitService.delete(id);
        return Result.ok();
    }

    /** 打卡 / 取消打卡 / 补打卡 */
    @PostMapping("/{id}/check")
    public Result<Boolean> check(@PathVariable Long id, @RequestBody HabitDto.CheckReq req) {
        return Result.ok(habitService.check(id, req));
    }

    @GetMapping("/calendar")
    public Result<HabitVo.Calendar> calendar(@RequestParam(required = false) String month) {
        return Result.ok(habitService.calendar(month));
    }

    @GetMapping("/stats")
    public Result<HabitVo.Stats> stats() {
        return Result.ok(habitService.stats());
    }
}

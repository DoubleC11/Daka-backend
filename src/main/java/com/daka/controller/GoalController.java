package com.daka.controller;

import com.daka.common.Result;
import com.daka.dto.GoalDto;
import com.daka.entity.Goal;
import com.daka.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public Result<List<Goal>> list() {
        return Result.ok(goalService.list());
    }

    @PostMapping
    public Result<Goal> create(@Valid @RequestBody GoalDto.SaveReq req) {
        return Result.ok(goalService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Goal> update(@PathVariable Long id, @Valid @RequestBody GoalDto.SaveReq req) {
        return Result.ok(goalService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goalService.delete(id);
        return Result.ok();
    }
}

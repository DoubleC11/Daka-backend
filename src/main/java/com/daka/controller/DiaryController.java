package com.daka.controller;

import com.daka.common.Result;
import com.daka.dto.DiaryDto;
import com.daka.entity.Diary;
import com.daka.service.DiaryService;
import com.daka.vo.DiaryVo;
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
@RequestMapping("/api/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @GetMapping
    public Result<List<Diary>> list(@RequestParam(required = false) String month,
                                    @RequestParam(required = false) String keyword) {
        return Result.ok(diaryService.list(month, keyword));
    }

    @PostMapping
    public Result<Diary> create(@Valid @RequestBody DiaryDto.SaveReq req) {
        return Result.ok(diaryService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Diary> update(@PathVariable Long id, @Valid @RequestBody DiaryDto.SaveReq req) {
        return Result.ok(diaryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        diaryService.delete(id);
        return Result.ok();
    }

    /** 心情分布 + 有记录的日子 */
    @GetMapping("/stats")
    public Result<DiaryVo.Stats> stats(@RequestParam(required = false) String month) {
        return Result.ok(diaryService.stats(month));
    }

    @GetMapping("/moods")
    public Result<List<String>> moods() {
        return Result.ok(DiaryService.MOODS);
    }
}

package com.daka.controller;

import com.daka.common.Result;
import com.daka.dto.BillDto;
import com.daka.entity.Bill;
import com.daka.service.BillService;
import com.daka.vo.BillVo;
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
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping
    public Result<List<Bill>> list(@RequestParam(required = false) String month,
                                   @RequestParam(required = false) Integer type) {
        return Result.ok(billService.list(month, type));
    }

    @PostMapping
    public Result<Bill> create(@Valid @RequestBody BillDto.SaveReq req) {
        return Result.ok(billService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Bill> update(@PathVariable Long id, @Valid @RequestBody BillDto.SaveReq req) {
        return Result.ok(billService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        billService.delete(id);
        return Result.ok();
    }

    /** 本月收支汇总 */
    @GetMapping("/summary")
    public Result<BillVo.Summary> summary(@RequestParam(required = false) String month) {
        return Result.ok(billService.summary(month));
    }

    /** 按分类占比，type 默认 2（支出） */
    @GetMapping("/by-category")
    public Result<List<BillVo.CategoryStat>> byCategory(@RequestParam(required = false) String month,
                                                        @RequestParam(required = false) Integer type) {
        return Result.ok(billService.byCategory(month, type));
    }
}

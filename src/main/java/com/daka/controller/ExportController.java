package com.daka.controller;

import com.daka.common.UserContext;
import com.daka.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

/**
 * 数据导出：CSV（UTF-8 BOM，Excel 友好）。
 * 返回体不走统一 JSON 包装，直接写文件流。
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/bills")
    public void bills(@RequestParam(required = false) String month,
                      @RequestParam(required = false) Integer type,
                      HttpServletResponse res) throws IOException {
        Long uid = UserContext.require();
        write(res, "账单", month, exportService.toCsvBytes(exportService.bills(uid, month, type)));
    }

    @GetMapping("/todos")
    public void todos(HttpServletResponse res) throws IOException {
        Long uid = UserContext.require();
        String suffix = LocalDate.now().toString();
        res.setContentType("text/csv;charset=UTF-8");
        exportService.setDownloadHeaders(res, "待办清单_" + suffix + ".csv");
        res.getOutputStream().write(exportService.toCsvBytes(exportService.todos(uid)));
    }

    @GetMapping("/diaries")
    public void diaries(@RequestParam(required = false) String month,
                        HttpServletResponse res) throws IOException {
        Long uid = UserContext.require();
        write(res, "心情日记", month, exportService.toCsvBytes(exportService.diaries(uid, month)));
    }

    @GetMapping("/habit-records")
    public void habitRecords(@RequestParam(required = false) String month,
                             HttpServletResponse res) throws IOException {
        Long uid = UserContext.require();
        write(res, "习惯打卡", month, exportService.toCsvBytes(exportService.habitRecords(uid, month)));
    }

    @GetMapping("/goals")
    public void goals(HttpServletResponse res) throws IOException {
        Long uid = UserContext.require();
        res.setContentType("text/csv;charset=UTF-8");
        exportService.setDownloadHeaders(res, "目标进度_" + LocalDate.now() + ".csv");
        res.getOutputStream().write(exportService.toCsvBytes(exportService.goals(uid)));
    }

    private void write(HttpServletResponse res, String name, String month, byte[] csv) throws IOException {
        res.setContentType("text/csv;charset=UTF-8");
        String suffix = (month == null || month.isBlank()) ? LocalDate.now().toString() : month;
        exportService.setDownloadHeaders(res, name + "_" + suffix + ".csv");
        res.getOutputStream().write(csv);
    }
}

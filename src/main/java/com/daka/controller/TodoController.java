package com.daka.controller;

import com.daka.common.Result;
import com.daka.dto.TodoDto;
import com.daka.entity.Todo;
import com.daka.service.TodoService;
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
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public Result<List<Todo>> list(@RequestParam(required = false) String scope) {
        return Result.ok(todoService.list(scope));
    }

    @PostMapping
    public Result<Todo> create(@Valid @RequestBody TodoDto.SaveReq req) {
        return Result.ok(todoService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Todo> update(@PathVariable Long id, @Valid @RequestBody TodoDto.SaveReq req) {
        return Result.ok(todoService.update(id, req));
    }

    @PutMapping("/{id}/toggle")
    public Result<Todo> toggle(@PathVariable Long id) {
        return Result.ok(todoService.toggle(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        todoService.delete(id);
        return Result.ok();
    }
}

package com.huyuhui.quiz.controller;

import com.huyuhui.quiz.common.ApiResult;
import com.huyuhui.quiz.entity.Question;
import com.huyuhui.quiz.service.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 题库接口（无需登录）
 */
@RestController
@RequestMapping("/api/meta")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /** 题库概览：题目总数、学科、章节 */
    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary() {
        List<String> subjects = questionService.subjects();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subjects", subjects);
        data.put("subjectCount", subjects.size());
        return ApiResult.ok(data);
    }

    /** 学科列表 */
    @GetMapping("/subjects")
    public ApiResult<List<String>> subjects() {
        return ApiResult.ok(questionService.subjects());
    }

    /** 某学科的章节列表 */
    @GetMapping("/chapters")
    public ApiResult<List<String>> chapters(@RequestParam String subject) {
        return ApiResult.ok(questionService.chapters(subject));
    }

    /**
     * 分页查询题目
     *
     * @param page 从 0 开始
     */
    @GetMapping("/questions")
    public ApiResult<Map<String, Object>> questions(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String chapter,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Question> result = questionService.search(subject, chapter, type, difficulty, keyword, page, size);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", result.getNumber());
        data.put("size", result.getSize());
        data.put("totalPages", result.getTotalPages());
        return ApiResult.ok(data);
    }

    /** 按学科取全部题目（练习用） */
    @GetMapping("/questions/by-subject")
    public ApiResult<List<Question>> bySubject(@RequestParam String subject) {
        return ApiResult.ok(questionService.bySubject(subject));
    }

    /** 随机取题（模拟考试用） */
    @GetMapping("/questions/random")
    public ApiResult<List<Question>> random(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String subject) {
        return ApiResult.ok(questionService.random(limit, subject));
    }

    /** 按 id 批量取题 */
    @PostMapping("/questions/batch")
    public ApiResult<List<Question>> batch(@RequestBody List<Long> ids) {
        return ApiResult.ok(questionService.byIds(ids));
    }
}
package com.huyuhui.quiz.controller;

import com.huyuhui.quiz.common.ApiResult;
import com.huyuhui.quiz.entity.ExamRecord;
import com.huyuhui.quiz.entity.User;
import com.huyuhui.quiz.security.AuthInterceptor;
import com.huyuhui.quiz.service.PracticeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 练习与学习数据接口（需登录）
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    public record SubmitRequest(Long questionId, Object answer) {}

    public record ExamRequest(String scope, Integer total, Integer correct, Integer durationSeconds) {}

    /** 提交单题答案 */
    @PostMapping("/submit")
    public ApiResult<Map<String, Object>> submit(@RequestAttribute(AuthInterceptor.ATTR_USER) User user,
                                                 @RequestBody SubmitRequest req) {
        return ApiResult.ok(practiceService.submit(user.getId(), req.questionId(), req.answer()));
    }

    /** 收藏 / 取消收藏，返回操作后的状态 */
    @PostMapping("/favorite/{questionId}")
    public ApiResult<Map<String, Object>> toggleFavorite(
            @RequestAttribute(AuthInterceptor.ATTR_USER) User user,
            @PathVariable Long questionId) {
        boolean favored = practiceService.toggleFavorite(user.getId(), questionId);
        return ApiResult.ok(Map.of("favorited", favored));
    }

    /** 错题本 */
    @GetMapping("/wrong")
    public ApiResult<List<Map<String, Object>>> wrong(@RequestAttribute(AuthInterceptor.ATTR_USER) User user) {
        return ApiResult.ok(practiceService.wrongList(user.getId()));
    }

    /** 收藏夹 */
    @GetMapping("/favorites")
    public ApiResult<List<Map<String, Object>>> favorites(
            @RequestAttribute(AuthInterceptor.ATTR_USER) User user) {
        return ApiResult.ok(practiceService.favoriteList(user.getId()));
    }

    /** 移除错题或收藏 */
    @DeleteMapping("/mark/{questionId}")
    public ApiResult<Void> removeMark(@RequestAttribute(AuthInterceptor.ATTR_USER) User user,
                                      @PathVariable Long questionId,
                                      @RequestParam String type) {
        practiceService.removeMark(user.getId(), questionId, type);
        return ApiResult.ok();
    }

    /** 学习统计 */
    @GetMapping("/statistics")
    public ApiResult<Map<String, Object>> statistics(@RequestAttribute(AuthInterceptor.ATTR_USER) User user) {
        return ApiResult.ok(practiceService.statistics(user.getId()));
    }

    /** 保存模拟考试成绩 */
    @PostMapping("/exam")
    public ApiResult<ExamRecord> saveExam(@RequestAttribute(AuthInterceptor.ATTR_USER) User user,
                                          @RequestBody ExamRequest req) {
        return ApiResult.ok(practiceService.saveExam(user.getId(), req.scope(),
                req.total() == null ? 0 : req.total(),
                req.correct() == null ? 0 : req.correct(),
                req.durationSeconds() == null ? 0 : req.durationSeconds()));
    }

    /** 考试历史 */
    @GetMapping("/exam/history")
    public ApiResult<List<ExamRecord>> examHistory(@RequestAttribute(AuthInterceptor.ATTR_USER) User user) {
        return ApiResult.ok(practiceService.examHistory(user.getId()));
    }
}
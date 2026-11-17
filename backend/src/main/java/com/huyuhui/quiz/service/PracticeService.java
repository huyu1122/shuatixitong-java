package com.huyuhui.quiz.service;

import com.huyuhui.quiz.entity.*;
import com.huyuhui.quiz.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 答题服务：提交答案、维护错题本、统计
 */
@Service
public class PracticeService {

    private final QuestionService questionService;
    private final QuestionRepository questionRepository;
    private final AnswerRecordRepository recordRepository;
    private final QuestionMarkRepository markRepository;
    private final ExamRecordRepository examRepository;

    public PracticeService(QuestionService questionService,
                           QuestionRepository questionRepository,
                           AnswerRecordRepository recordRepository,
                           QuestionMarkRepository markRepository,
                           ExamRecordRepository examRepository) {
        this.questionService = questionService;
        this.questionRepository = questionRepository;
        this.recordRepository = recordRepository;
        this.markRepository = markRepository;
        this.examRepository = examRepository;
    }

    /**
     * 提交单题答案并返回判分结果
     */
    @Transactional
    public Map<String, Object> submit(Long userId, Long questionId, Object answer) {
        Question question = questionService.getById(questionId);
        boolean correct = questionService.judge(question, answer);

        // 更新答题记录（不存在则新建）
        AnswerRecord record = recordRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseGet(() -> {
                    AnswerRecord r = new AnswerRecord();
                    r.setUserId(userId);
                    r.setQuestionId(questionId);
                    return r;
                });
        record.setAttempts(record.getAttempts() + 1);
        if (correct) {
            record.setCorrectCount(record.getCorrectCount() + 1);
        }
        record.setLastCorrect(correct);
        record.setLastAnswer(String.valueOf(answer));
        record.setLastAt(LocalDateTime.now());
        recordRepository.save(record);

        // 答错则加入错题本；答对则把错题标记为已订正
        if (!correct) {
            QuestionMark mark = markRepository
                    .findByUserIdAndQuestionIdAndType(userId, questionId, "WRONG")
                    .orElseGet(() -> {
                        QuestionMark m = new QuestionMark();
                        m.setUserId(userId);
                        m.setQuestionId(questionId);
                        m.setType("WRONG");
                        return m;
                    });
            mark.setResolved(false);
            markRepository.save(mark);
        } else {
            markRepository.findByUserIdAndQuestionIdAndType(userId, questionId, "WRONG")
                    .ifPresent(m -> {
                        m.setResolved(true);
                        markRepository.save(m);
                    });
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", correct);
        result.put("correctAnswer", question.getAnswer());
        result.put("analysis", question.getAnalysis());
        result.put("attempts", record.getAttempts());
        result.put("correctCount", record.getCorrectCount());
        return result;
    }

    /** 收藏 / 取消收藏 */
    @Transactional
    public boolean toggleFavorite(Long userId, Long questionId) {
        var existing = markRepository.findByUserIdAndQuestionIdAndType(userId, questionId, "FAVORITE");
        if (existing.isPresent()) {
            markRepository.delete(existing.get());
            return false;
        }
        QuestionMark mark = new QuestionMark();
        mark.setUserId(userId);
        mark.setQuestionId(questionId);
        mark.setType("FAVORITE");
        markRepository.save(mark);
        return true;
    }

    /** 错题列表（带题目内容） */
    public List<Map<String, Object>> wrongList(Long userId) {
        return markWithQuestions(userId, "WRONG");
    }

    /** 收藏列表 */
    public List<Map<String, Object>> favoriteList(Long userId) {
        return markWithQuestions(userId, "FAVORITE");
    }

    /** 移除错题 / 收藏 */
    @Transactional
    public void removeMark(Long userId, Long questionId, String type) {
        markRepository.findByUserIdAndQuestionIdAndType(userId, questionId, type)
                .ifPresent(markRepository::delete);
    }

    private List<Map<String, Object>> markWithQuestions(Long userId, String type) {
        List<QuestionMark> marks = markRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
        if (marks.isEmpty()) return List.of();

        List<Long> ids = marks.stream().map(QuestionMark::getQuestionId).toList();
        Map<Long, Question> qmap = questionRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Map<String, Object>> out = new ArrayList<>();
        for (QuestionMark m : marks) {
            Question q = qmap.get(m.getQuestionId());
            if (q == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", q);
            item.put("resolved", m.getResolved());
            item.put("markedAt", m.getCreatedAt().toString());
            out.add(item);
        }
        return out;
    }

    /**
     * 学习统计
     */
    public Map<String, Object> statistics(Long userId) {
        long attempts = recordRepository.sumAttemptsByUser(userId);
        long correct = recordRepository.sumCorrectByUser(userId);
        long practiced = recordRepository.countByUserId(userId);
        long totalQuestions = questionRepository.count();

        double accuracy = attempts == 0 ? 0.0 : Math.round(correct * 1000.0 / attempts) / 10.0;

        // 学科维度正确率
        Map<Long, Question> qmap = questionRepository.findAll().stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        Map<String, int[]> bySubject = new LinkedHashMap<>();
        for (AnswerRecord r : recordRepository.findByUserId(userId)) {
            Question q = qmap.get(r.getQuestionId());
            if (q == null) continue;
            int[] acc = bySubject.computeIfAbsent(q.getSubject(), k -> new int[2]);
            acc[0] += r.getAttempts();
            acc[1] += r.getCorrectCount();
        }
        List<Map<String, Object>> subjectStats = new ArrayList<>();
        bySubject.forEach((subject, acc) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("subject", subject);
            item.put("attempts", acc[0]);
            item.put("correct", acc[1]);
            item.put("accuracy", acc[0] == 0 ? 0.0 : Math.round(acc[1] * 1000.0 / acc[0]) / 10.0);
            subjectStats.add(item);
        });
        subjectStats.sort((a, b) -> Double.compare((double) b.get("attempts"), (double) a.get("attempts")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAttempts", attempts);
        result.put("totalCorrect", correct);
        result.put("accuracy", accuracy);
        result.put("practicedQuestions", practiced);
        result.put("totalQuestions", totalQuestions);
        result.put("wrongCount", markRepository.countByUserIdAndTypeAndResolved(userId, "WRONG", false));
        result.put("favoriteCount", markRepository.countByUserIdAndTypeAndResolved(userId, "FAVORITE", false));
        result.put("examCount", examRepository.countByUserId(userId));
        result.put("subjectStats", subjectStats);
        result.put("today", LocalDate.now().toString());
        return result;
    }

    /** 保存模拟考试成绩 */
    @Transactional
    public ExamRecord saveExam(Long userId, String scope, int total, int correct, int durationSeconds) {
        ExamRecord exam = new ExamRecord();
        exam.setUserId(userId);
        exam.setScope(scope);
        exam.setTotal(total);
        exam.setCorrectCount(correct);
        exam.setScore(total == 0 ? 0 : Math.round(correct * 100.0f / total));
        exam.setDurationSeconds(durationSeconds);
        return examRepository.save(exam);
    }

    public List<ExamRecord> examHistory(Long userId) {
        return examRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
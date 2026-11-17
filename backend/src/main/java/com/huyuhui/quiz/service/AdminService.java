package com.huyuhui.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huyuhui.quiz.common.BizException;
import com.huyuhui.quiz.entity.Question;
import com.huyuhui.quiz.entity.User;
import com.huyuhui.quiz.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 后台管理服务：题目管理、用户管理
 */
@Service
public class AdminService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AnswerRecordRepository recordRepository;
    private final QuestionMarkRepository markRepository;
    private final SessionRepository sessionRepository;
    private final ExamRecordRepository examRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public AdminService(QuestionRepository questionRepository,
                        UserRepository userRepository,
                        AnswerRecordRepository recordRepository,
                        QuestionMarkRepository markRepository,
                        SessionRepository sessionRepository,
                        ExamRecordRepository examRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.markRepository = markRepository;
        this.sessionRepository = sessionRepository;
        this.examRepository = examRepository;
    }

    // ==================== 题目管理 ====================

    /** 分页查询题目（含答案与解析，管理端需要） */
    public Page<Question> listQuestions(String subject, String keyword, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "id"));
        return questionRepository.search(
                blank(subject), null, null, null, blank(keyword), pr);
    }

    /** 新增题目 */
    @Transactional
    public Question createQuestion(Map<String, Object> body) {
        Question q = new Question();
        q.setCode(uniqueCode(body.get("code")));
        applyFields(q, body);
        validate(q);
        return questionRepository.save(q);
    }

    /** 修改题目 */
    @Transactional
    public Question updateQuestion(Long id, Map<String, Object> body) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BizException("题目不存在"));
        if (body.containsKey("code")) {
            String newCode = String.valueOf(body.get("code")).trim();
            if (!newCode.equals(q.getCode())) {
                if (questionRepository.existsByCode(newCode)) {
                    throw new BizException("题目编码已存在：" + newCode);
                }
                q.setCode(newCode);
            }
        }
        applyFields(q, body);
        validate(q);
        return questionRepository.save(q);
    }

    /** 删除题目（级联删除答题记录与标记） */
    @Transactional
    public void deleteQuestion(Long id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new BizException("题目不存在"));
        questionRepository.delete(q);
    }

    /** 批量删除 */
    @Transactional
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<Question> list = questionRepository.findByIdIn(ids);
        questionRepository.deleteAll(list);
        return list.size();
    }

    /** 学科与章节统计，供后台下拉框与概览使用 */
    public Map<String, Object> questionStats() {
        List<Question> all = questionRepository.findAll();
        Map<String, Map<String, Long>> tree = new LinkedHashMap<>();
        Map<String, Long> byType = new LinkedHashMap<>();
        Map<Integer, Long> byDifficulty = new LinkedHashMap<>();

        for (Question q : all) {
            tree.computeIfAbsent(q.getSubject(), k -> new LinkedHashMap<>())
                    .merge(q.getChapter(), 1L, Long::sum);
            byType.merge(q.getType(), 1L, Long::sum);
            byDifficulty.merge(q.getDifficulty(), 1L, Long::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("subjectCount", tree.size());
        result.put("chapterCount", tree.values().stream().mapToInt(Map::size).sum());
        result.put("byType", byType);
        result.put("byDifficulty", byDifficulty);
        result.put("tree", tree);
        return result;
    }

    // ==================== 用户管理 ====================

    /** 用户列表（带答题数） */
    public List<Map<String, Object>> listUsers(String keyword) {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        String kw = (keyword == null) ? "" : keyword.trim();

        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : users) {
            if (!kw.isEmpty()
                    && !u.getUsername().contains(kw)
                    && !(u.getNickname() != null && u.getNickname().contains(kw))) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("nickname", u.getNickname());
            item.put("role", u.getRole());
            item.put("createdAt", u.getCreatedAt() == null ? null : u.getCreatedAt().toString());
            item.put("locked",
                    u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now()));
            item.put("attempts", recordRepository.sumAttemptsByUser(u.getId()));
            out.add(item);
        }
        return out;
    }

    /** 修改用户角色 */
    @Transactional
    public User setRole(Long userId, String role, Long operatorId) {
        if (!"ADMIN".equals(role) && !"STUDENT".equals(role)) {
            throw new BizException("角色只能是 ADMIN 或 STUDENT");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        // 不允许管理员把自己降级，避免把自己锁在系统外
        if (user.getId().equals(operatorId) && !"ADMIN".equals(role)) {
            throw new BizException("不能修改自己的管理员角色");
        }
        user.setRole(role);
        return userRepository.save(user);
    }

    /** 解锁用户 */
    @Transactional
    public User unlock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        return userRepository.save(user);
    }

    /** 重置用户密码 */
    @Transactional
    public void resetPassword(Long userId, String newPassword,
                              org.springframework.security.crypto.password.PasswordEncoder encoder) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("密码至少 6 个字符");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        user.setPassword(encoder.encode(newPassword));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        // 密码变更后强制该用户重新登录
        sessionRepository.deleteByUserId(userId);
    }

    /** 删除用户（级联删除其学习数据） */
    @Transactional
    public void deleteUser(Long userId, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new BizException("不能删除自己的账号");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        userRepository.delete(user);
    }

    /** 后台总览数据 */
    public Map<String, Object> dashboard() {
        List<User> users = userRepository.findAll();
        long admins = users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", users.size());
        result.put("adminCount", admins);
        result.put("questionCount", questionRepository.count());
        result.put("examCount", examRepository.count());
        result.put("recordCount", recordRepository.count());
        result.put("markCount", markRepository.count());

        // 近 7 天新增用户
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long newUsers = users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(weekAgo))
                .count();
        result.put("newUsersThisWeek", newUsers);
        return result;
    }

    // ==================== 内部工具 ====================

    /** 未指定编码时自动生成，如 q-101 */
    private String uniqueCode(Object raw) {
        if (raw != null && !String.valueOf(raw).isBlank()) {
            String code = String.valueOf(raw).trim();
            if (questionRepository.existsByCode(code)) {
                throw new BizException("题目编码已存在：" + code);
            }
            return code;
        }
        int seq = 1;
        while (questionRepository.existsByCode("q-" + seq)) seq++;
        return "q-" + seq;
    }

    /** 把请求体字段写入实体（未提供的字段保持原值） */
    private void applyFields(Question q, Map<String, Object> body) {
        if (body.containsKey("subject")) q.setSubject(str(body.get("subject"), q.getSubject()));
        if (body.containsKey("chapter")) q.setChapter(str(body.get("chapter"), q.getChapter()));
        if (body.containsKey("type")) q.setType(str(body.get("type"), q.getType()));
        if (body.containsKey("difficulty")) {
            Object d = body.get("difficulty");
            if (d instanceof Number n) q.setDifficulty(n.intValue());
        }
        if (body.containsKey("stem")) q.setStem(str(body.get("stem"), q.getStem()));
        if (body.containsKey("analysis")) q.setAnalysis(str(body.get("analysis"), null));
        if (body.containsKey("options")) q.setOptions(toJsonOrNull(body.get("options")));
        if (body.containsKey("answer")) q.setAnswer(toJsonRequired(body.get("answer")));
        if (body.containsKey("knowledge")) q.setKnowledge(toJsonOrNull(body.get("knowledge")));
    }

    /** 数据完整性校验 */
    private void validate(Question q) {
        if (q.getStem() == null || q.getStem().isBlank()) {
            throw new BizException("题干不能为空");
        }
        if (q.getSubject() == null || q.getSubject().isBlank()) {
            throw new BizException("学科不能为空");
        }
        if (q.getChapter() == null || q.getChapter().isBlank()) {
            throw new BizException("章节不能为空");
        }
        String type = q.getType();
        if (type == null || !List.of("single", "multiple", "judge", "fill").contains(type)) {
            throw new BizException("题型只能是 single / multiple / judge / fill");
        }
        if (q.getAnswer() == null || q.getAnswer().isBlank()) {
            throw new BizException("答案不能为空");
        }

        // 选项题必须至少有 2 个选项，且答案下标在范围内
        if (!"fill".equals(type)) {
            List<Object> opts = parseList(q.getOptions());
            if (opts.size() < 2) {
                throw new BizException("选择题至少需要 2 个选项");
            }
            List<Object> ans = parseList(q.getAnswer());
            if (ans.isEmpty()) {
                throw new BizException("请设置正确答案");
            }
            for (Object a : ans) {
                int idx = (a instanceof Number n) ? n.intValue() : -1;
                if (idx < 0 || idx >= opts.size()) {
                    throw new BizException("答案下标超出选项范围");
                }
            }
            if ("single".equals(type) && ans.size() != 1) {
                throw new BizException("单选题只能有 1 个正确答案");
            }
        }
    }

    private List<Object> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String str(Object v, String fallback) {
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? fallback : s;
    }

    private String toJsonOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof String s) {
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }
        try {
            return mapper.writeValueAsString(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJsonRequired(Object v) {
        if (v == null) return null;
        if (v instanceof String s) return s.trim();
        try {
            return mapper.writeValueAsString(v);
        } catch (Exception e) {
            return null;
        }
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
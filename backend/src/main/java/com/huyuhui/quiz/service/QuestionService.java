package com.huyuhui.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huyuhui.quiz.common.BizException;
import com.huyuhui.quiz.entity.Question;
import com.huyuhui.quiz.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 题目服务：查询、筛选、判分
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Page<Question> search(String subject, String chapter, String type,
                                 Integer difficulty, String keyword, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "id"));
        return questionRepository.search(blankToNull(subject), blankToNull(chapter),
                blankToNull(type), difficulty, blankToNull(keyword), pr);
    }

    public Question getById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new BizException("题目不存在"));
    }

    public List<String> subjects() {
        return questionRepository.findDistinctSubjects();
    }

    public List<String> chapters(String subject) {
        if (subject == null || subject.isBlank()) return List.of();
        return questionRepository.findDistinctChaptersBySubject(subject);
    }

    public List<Question> bySubject(String subject) {
        return questionRepository.findBySubjectOrderByIdAsc(subject);
    }

    public List<Question> byIds(List<Long> ids) {
        return ids.isEmpty() ? List.of() : questionRepository.findByIdIn(ids);
    }

    public List<Question> random(int limit, String subject) {
        int n = Math.min(Math.max(limit, 1), 100);
        List<Question> all = (subject == null || subject.isBlank())
                ? questionRepository.findAll()
                : bySubject(subject.trim());
        Collections.shuffle(all, new Random());
        return all.subList(0, Math.min(n, all.size()));
    }

    /**
     * 判分
     *
     * @param question 题目
     * @param submitted 用户提交的答案
     * @return true 表示答对
     */
    public boolean judge(Question question, Object submitted) {
        String type = question.getType();

        if ("fill".equals(type)) {
            // 填空题：字符串忽略首尾空白与大小写比对
            String expected = parseFillAnswer(question.getAnswer());
            String actual = String.valueOf(submitted == null ? "" : submitted).trim();
            return !actual.isEmpty() && actual.equalsIgnoreCase(expected.trim());
        }

        // 选择题 / 判断题：比较选项下标集合
        Set<Integer> expected = parseIndexSet(question.getAnswer());
        Set<Integer> actual = parseIndexSet(toJson(submitted));
        return !expected.isEmpty() && expected.equals(actual);
    }

    /** 填空题答案：存的是纯文本或 JSON 字符串，统一取出文本 */
    private String parseFillAnswer(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        try {
            if (s.startsWith("\"")) {
                return mapper.readValue(s, String.class);
            }
            if (s.startsWith("[")) {
                List<String> list = mapper.readValue(s, new TypeReference<List<String>>() {});
                return list.isEmpty() ? "" : list.get(0);
            }
        } catch (Exception ignore) {
            // 解析失败时按原文比对
        }
        return s;
    }

    /** 把 JSON 数组（如 [1] 或 ["1"]）解析为下标集合 */
    private Set<Integer> parseIndexSet(String raw) {
        Set<Integer> out = new TreeSet<>();
        if (raw == null || raw.isBlank()) return out;
        String s = raw.trim();
        try {
            if (s.startsWith("[")) {
                List<Object> list = mapper.readValue(s, new TypeReference<List<Object>>() {});
                for (Object o : list) {
                    if (o instanceof Number n) {
                        out.add(n.intValue());
                    } else if (o instanceof String str) {
                        try { out.add(Integer.parseInt(str.trim())); } catch (NumberFormatException ignore) {}
                    }
                }
            } else {
                out.add(Integer.parseInt(s));
            }
        } catch (Exception ignore) {
            // 非法输入视为空集合 → 判错
        }
        return out;
    }

    private String toJson(Object o) {
        if (o == null) return null;
        if (o instanceof String s) return s;
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
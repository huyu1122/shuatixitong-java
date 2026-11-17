package com.huyuhui.quiz.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huyuhui.quiz.entity.Question;
import com.huyuhui.quiz.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 题库初始化：应用启动时若题目表为空，则从 classpath:data/questions.json 导入
 */
@Component
public class QuestionSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(QuestionSeeder.class);

    private final QuestionRepository questionRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public QuestionSeeder(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        long existing = questionRepository.count();
        if (existing > 0) {
            log.info("题库已存在 {} 道题，跳过导入", existing);
            return;
        }

        ClassPathResource res = new ClassPathResource("data/questions.json");
        if (!res.exists()) {
            log.warn("未找到 data/questions.json，跳过题库导入");
            return;
        }

        int imported = 0;
        try (InputStream in = res.getInputStream()) {
            JsonNode root = mapper.readTree(in);
            for (JsonNode node : root) {
                String code = node.path("id").asText(null);
                if (code == null || questionRepository.existsByCode(code)) {
                    continue;
                }
                Question q = new Question();
                q.setCode(code);
                q.setSubject(node.path("subject").asText("未分类"));
                q.setChapter(node.path("chapter").asText("未分类"));
                q.setType(node.path("type").asText("single"));
                q.setDifficulty(node.path("difficulty").asInt(1));
                q.setStem(node.path("stem").asText(""));
                q.setOptions(node.has("options") ? mapper.writeValueAsString(node.get("options")) : null);
                q.setAnswer(mapper.writeValueAsString(node.get("answer")));
                q.setAnalysis(node.path("analysis").asText(null));
                q.setKnowledge(node.has("knowledge")
                        ? mapper.writeValueAsString(node.get("knowledge")) : null);
                questionRepository.save(q);
                imported++;
            }
        } catch (Exception e) {
            log.error("题库导入失败", e);
            return;
        }

        log.info("题库导入完成，共 {} 道题", imported);
    }
}
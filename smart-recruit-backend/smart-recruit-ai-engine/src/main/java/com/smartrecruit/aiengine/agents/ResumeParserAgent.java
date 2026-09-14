package com.smartrecruit.aiengine.agents;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 简历解析智能体——模拟AI驱动的简历解析。
 *
 * <p>从简历文件中提取结构化的候选人画像数据。
 * 在模拟模式下，生成逼真的随机数据。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
public class ResumeParserAgent {

    private static final List<String> FAKE_SCHOOLS = List.of(
            "Tsinghua University", "Peking University", "Zhejiang University",
            "Fudan University", "Shanghai Jiao Tong University", "Nanjing University",
            "University of Science and Technology of China", "Wuhan University"
    );

    private static final List<String> FAKE_MAJORS = List.of(
            "Computer Science", "Software Engineering", "Data Science",
            "Artificial Intelligence", "Information Systems", "Electrical Engineering"
    );

    private static final List<String> FAKE_SKILLS = List.of(
            "Java", "Python", "Go", "Rust", "TypeScript", "React", "Spring Boot",
            "Kubernetes", "Docker", "MySQL", "Redis", "Kafka", "Elasticsearch",
            "TensorFlow", "PyTorch", "AWS", "GCP", "CI/CD", "Terraform"
    );

    private static final List<String> FAKE_COMPANIES = List.of(
            "Alibaba", "Tencent", "ByteDance", "Meituan", "JD.com",
            "Huawei", "Baidu", "Xiaomi", "NetEase", "Pinduoduo"
    );

    private static final List<String> FAKE_CERTS = List.of(
            "AWS Solutions Architect", "Google Cloud Professional",
            "PMP", "CKAD", "CKA", "TOGAF", "Scrum Master"
    );

    private static final List<String> FAKE_LANGUAGES = List.of(
            "Mandarin (Native)", "English (Fluent)", "Japanese (Intermediate)",
            "Korean (Basic)"
    );

    /**
     * 模拟解析简历文件并返回结构化的候选人数据。
     *
     * @param fileName 模拟文件名（真实实现将接收MultipartFile）
     * @return 结构化的候选人画像
     */
    public CandidateProfile parse(String fileName) {
        log.info("ResumeParserAgent: parsing resume file={}", fileName);
        simulateLatency(100, 400);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int skillCount = rnd.nextInt(4, 10);
        List<String> skills = new ArrayList<>();
        Map<String, Integer> proficiency = new LinkedHashMap<>();
        for (int i = 0; i < skillCount; i++) {
            String skill = FAKE_SKILLS.get(rnd.nextInt(FAKE_SKILLS.size()));
            if (!skills.contains(skill)) {
                skills.add(skill);
                proficiency.put(skill, rnd.nextInt(60, 100));
            }
        }

        int certCount = rnd.nextInt(0, 3);
        List<String> certs = new ArrayList<>();
        for (int i = 0; i < certCount; i++) {
            String cert = FAKE_CERTS.get(rnd.nextInt(FAKE_CERTS.size()));
            if (!certs.contains(cert)) certs.add(cert);
        }

        int langCount = rnd.nextInt(1, 3);
        List<String> langs = new ArrayList<>();
        for (int i = 0; i < langCount; i++) {
            langs.add(FAKE_LANGUAGES.get(rnd.nextInt(FAKE_LANGUAGES.size())));
        }

        CandidateProfile profile = CandidateProfile.builder()
                .name("Simulated Candidate")
                .email("candidate_" + DateUtils.currentEpochMillis() + "@example.com")
                .phone("138" + String.format("%08d", rnd.nextInt(100_000_000)))
                .educationLevel(rnd.nextBoolean() ? "MASTER" : "BACHELOR")
                .school(FAKE_SCHOOLS.get(rnd.nextInt(FAKE_SCHOOLS.size())))
                .major(FAKE_MAJORS.get(rnd.nextInt(FAKE_MAJORS.size())))
                .yearsOfExperience(rnd.nextInt(1, 15))
                .currentCompany(FAKE_COMPANIES.get(rnd.nextInt(FAKE_COMPANIES.size())))
                .currentPosition(rnd.nextBoolean() ? "Senior Engineer" : "Staff Engineer")
                .skills(skills)
                .skillProficiency(proficiency)
                .certifications(certs)
                .languages(langs)
                .summary("Experienced software engineer with " +
                        skills.size() + " technical skills and strong problem-solving abilities.")
                .overallScore(rnd.nextDouble(65.0, 98.0))
                .build();

        log.info("ResumeParserAgent: parsing complete, {} skills extracted", skills.size());
        return profile;
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

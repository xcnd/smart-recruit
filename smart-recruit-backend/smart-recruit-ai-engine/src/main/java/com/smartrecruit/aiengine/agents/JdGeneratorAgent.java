package com.smartrecruit.aiengine.agents;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JD 生成智能体。
 *
 * <p>根据岗位名称、部门与经验要求生成结构化 JD
 * （岗位职责 / 任职要求 / 加分项）。LLM 不可用时作为降级实现，
 * 也可作为 Agent 能力网关的兜底。</p>
 *
 * @since 2026-04-06
 */
@Component
@Slf4j
public class JdGeneratorAgent {

    /**
     * 生成结构化 JD。
     *
     * @param jobTitle      岗位名称
     * @param department    部门名称
     * @param experienceLevel 经验要求（如 "3-5年"）
     * @return {responsibilities, requirements, plusPoints}
     */
    public Map<String, Object> generate(String jobTitle, String department, String experienceLevel) {
        log.info("JdGeneratorAgent 生成 JD: title={}, department={}, exp={}",
                jobTitle, department, experienceLevel);

        List<String> responsibilities = new ArrayList<>(List.of(
                "负责" + safe(jobTitle) + "相关核心业务的设计、开发与交付，保证代码质量与系统稳定",
                "参与技术方案评审与架构演进，输出高质量设计文档",
                "与产品、测试等团队高效协作，推动项目按计划落地",
                "持续优化系统性能与工程效率，解决线上疑难问题"));

        List<String> requirements = new ArrayList<>(List.of(
                "本科及以上学历，计算机相关专业优先",
                "具备 " + safe(experienceLevel) + " 及以上相关岗位经验",
                "扎实的计算机基础与编程能力，熟悉主流技术栈",
                "良好的沟通协作与自我驱动力"));

        List<String> plusPoints = new ArrayList<>(List.of(
                "有大型分布式系统或高并发项目经验",
                "有开源项目贡献或技术博客",
                "熟悉" + safe(department) + "相关领域知识"));

        Map<String, Object> jd = new LinkedHashMap<>();
        jd.put("title", jobTitle);
        jd.put("department", department);
        jd.put("experienceLevel", experienceLevel);
        jd.put("responsibilities", responsibilities);
        jd.put("requirements", requirements);
        jd.put("plusPoints", plusPoints);
        return jd;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "岗位" : value.trim();
    }
}

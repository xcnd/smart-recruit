package com.smartrecruit.aiengine.service.impl;

import com.smartrecruit.aiengine.entity.AiResumeParseLog;
import com.smartrecruit.aiengine.repository.AiResumeParseLogMapper;
import com.smartrecruit.aiengine.service.ResumeParseLogRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * {@link ResumeParseLogRecorder} 实现类。
 *
 * @since 2026-09-21
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeParseLogRecorderImpl implements ResumeParseLogRecorder {

    /** 日志状态：成功，对应 {@code ai_resume_parse_log.status}。 */
    private static final int STATUS_SUCCESS = 0;

    /** 日志状态：失败，对应 {@code ai_resume_parse_log.status}。 */
    private static final int STATUS_FAILED = 1;

    private final AiResumeParseLogMapper resumeParseLogMapper;

    @Override
    public void record(Long resumeId, String engine, String model,
                       Integer inputTokens, Integer outputTokens,
                       long durationMs, boolean success, String errorMsg) {
        if (resumeId == null) {
            return;
        }
        try {
            int in = inputTokens == null ? 0 : inputTokens;
            int out = outputTokens == null ? 0 : outputTokens;

            AiResumeParseLog row = new AiResumeParseLog();
            row.setResumeId(resumeId);
            row.setEngine(engine);
            row.setModel(model);
            row.setInputTokens(in);
            row.setOutputTokens(out);
            row.setTotalTokens(in + out);
            row.setDurationMs(durationMs);
            // 费用按厂商单价由离线统计脚本换算，落库不做汇率/单价耦合
            row.setCostUsd(BigDecimal.ZERO);
            row.setStatus(success ? STATUS_SUCCESS : STATUS_FAILED);
            row.setErrorMsg(errorMsg);

            resumeParseLogMapper.insert(row);
        } catch (Exception e) {
            // 埋点失败不得影响解析主流程
            log.warn("简历解析调用日志写入失败（可忽略）: resumeId={}, error={}", resumeId, e.getMessage());
        }
    }
}

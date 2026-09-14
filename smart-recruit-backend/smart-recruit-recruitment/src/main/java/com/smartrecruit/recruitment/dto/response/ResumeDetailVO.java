package com.smartrecruit.recruitment.dto.response;

import com.smartrecruit.recruitment.domain.ParsedResume;
import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单份简历的详细视图对象。
 *
 * @since 1.0.0
 */
@Data
public class ResumeDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 简历 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;
    /** 候选人姓名。 */
    private String candidateName;
    /** 候选人头像 URL。 */
    private String avatarUrl;
    /** 职位 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobId;
    /** 职位名称。 */
    private String jobTitle;
    /** 文件名。 */
    private String fileName;
    /** 文件存储 URL。 */
    private String fileUrl;
    /** 文件类型。 */
    private Integer fileType;
    /** 文件大小（字节）。 */
    private Long fileSize;
    /** AI 匹配分数（0-100）。 */
    private BigDecimal matchScore;
    /** 技能标签列表。 */
    private List<String> skills;
    /** AI 解析后的结构化简历内容。 */
    private ParsedResume parsedContent;
    /** 解析状态。 */
    private Integer parseStatus;
    /** 解析失败原因详情（仅在 parseStatus=FAILED 时有值）。 */
    private String parseError;
    /** 上传时间。 */
    private LocalDateTime createdAt;

    /** 来源渠道：0=主动投递,1=内推,2=官网,3=LinkedIn,4=BOSS直聘,5=拉勾,6=猎聘,7=其他。 */
    private Integer source;
    /** 推荐人用户 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long referrerId;
    /** 推荐人姓名。 */
    private String referrerName;
    /** 推荐人所在部门。 */
    private String referrerDepartment;
    /** 推荐人职位（角色名称）。 */
    private String referrerPosition;

    /** 最新的AI筛选结果（如有）。 */
    private AiScreeningResultVO aiResult;
}

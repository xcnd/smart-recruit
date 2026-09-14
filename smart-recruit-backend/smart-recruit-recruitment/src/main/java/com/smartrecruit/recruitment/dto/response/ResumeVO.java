package com.smartrecruit.recruitment.dto.response;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历列表展示的视图对象。
 *
 * @since 1.0.0
 */
@Data
public class ResumeVO implements Serializable {

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
    /** 职位 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobId;
    /** 职位名称。 */
    private String jobTitle;
    /** 文件名。 */
    private String fileName;
    /** 文件存储 URL。 */
    private String fileUrl;
    /** 文件类型：PDF、DOCX、TXT。 */
    private Integer fileType;
    /** 文件大小（字节）。 */
    private Long fileSize;
    /** 解析状态：PENDING、PARSING、COMPLETED、FAILED。 */
    private Integer parseStatus;
    /** 筛选状态：0=待处理,1=已通过,2=已淘汰。 */
    private Integer screeningStatus;
    /** 上传后是否自动执行 AI 筛选。 */
    private Boolean autoScreen;
    /** AI 匹配分数（0-100）。 */
    private BigDecimal matchScore;
    /** 技能标签列表。 */
    private List<String> skills;
    /** 来源渠道：0=主动投递,1=内推,2=官网,3=LinkedIn,4=BOSS直聘,5=拉勾,6=猎聘,7=其他。 */
    private Integer source;
    /** 上传时间。 */
    private LocalDateTime createdAt;
}

package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.smartrecruit.recruitment.domain.ParsedResume;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历实体，映射 {@code rec_resume} 表。
 *
 * <p>表示候选人上传的简历文件，包含 AI 解析
 * 的内容和解析状态。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_resume", autoResultMap = true)
public class Resume implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 {@code rec_candidate.id}。 */
    private Long candidateId;

    /** 关联 {@code rec_job_position.id}。 */
    @TableField("job_position_id")
    private Long jobPositionId;

    /** AI 综合匹配度（0-100）。 */
    @TableField("ai_match_score")
    private java.math.BigDecimal aiMatchScore;

    /** 上传时的原始文件名，如 "resume_v3.pdf"。 */
    @TableField("file_name")
    private String fileName;

    /** OSS / 存储文件路径。 */
    @TableField("file_path")
    private String fileUrl;

    /** 文件扩展名：PDF、DOCX、TXT。 */
    @TableField("file_type")
    private Integer fileType;

    /** 文件大小（字节）。 */
    @TableField("file_size")
    private Long fileSize;

    /** AI 解析的结构化简历内容（教育背景、工作经历、技能等）。 */
    @TableField(value = "parsed_content", typeHandler = JacksonTypeHandler.class)
    private ParsedResume parsedContent;

    /** 解析状态：PENDING、PARSING、SUCCESS、FAILED。 */
    @TableField("parse_status")
    private Integer parseStatus;

    /** 解析失败原因（异常类型+消息+堆栈），仅在 parse_status=FAILED 时有值。 */
    @TableField("parse_error")
    private String parseError;

    /** 筛选状态：0=待处理,1=已通过,2=已淘汰。 */
    @TableField("screening_status")
    private Integer screeningStatus;

    /** 上传后是否自动执行 AI 筛选：true=开启（默认），false=关闭。 */
    @TableField("auto_screen")
    private Boolean autoScreen;

    /** 记录创建时间。 */
    @TableField("create_time")
    private LocalDateTime createdAt;

    /** 创建人ID。 */
    @TableField("create_user_id")
    private Long createUserId;

    /** 创建人用户名。 */
    @TableField("create_by")
    private String createBy;

    /** 更新人ID。 */
    @TableField("update_user_id")
    private Long updateUserId;

    /** 更新人用户名。 */
    @TableField("update_by")
    private String updateBy;
}

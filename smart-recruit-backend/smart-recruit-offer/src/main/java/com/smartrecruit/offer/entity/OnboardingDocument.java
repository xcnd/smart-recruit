package com.smartrecruit.offer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 入职文档实体，映射 {@code rec_onboarding_document} 表。
 *
 * <p>跟踪员工入职所需的文档，如劳动合同、保密协议、身份证明和资质证书。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_onboarding_document")
public class OnboardingDocument implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 入职流程 ID。 */
    private Long onboardingId;

    /** 文档类型：CONTRACT、NDA、ID、CERTIFICATION、PHOTO、DEGREE。 */
    private Integer docType;

    /** 文档显示名称。 */
    private String docName;

    /** 文档状态：PENDING、SUBMITTED、VERIFIED、REJECTED。 */
    private Integer status;

    /** 文件存储路径。 */
    private String filePath;

    /** 核实此文档的用户 ID。 */
    private Long verifiedBy;

    /** 文档核实时间戳。 */
    private LocalDateTime verifiedTime;

    /** 文档备注或说明。 */
    private String remark;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID。 */
    private Long createUserId;

    /** 创建人用户名。 */
    private String createBy;

    /** 更新人ID。 */
    private Long updateUserId;

    /** 更新人用户名。 */
    private String updateBy;
}

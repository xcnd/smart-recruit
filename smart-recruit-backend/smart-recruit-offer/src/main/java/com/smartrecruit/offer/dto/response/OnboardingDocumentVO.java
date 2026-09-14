package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 入职文档结构化视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDocumentVO {

    /** 文档记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 文档类型码：0=身份证,1=学历证明,2=离职证明,3=体检报告,4=银行卡,5=证件照。 */
    private Integer docType;

    /** 文档名称。 */
    private String docName;

    /** 状态码：0=缺失,1=待审核,2=已审核。 */
    private Integer status;

    /** 状态中文标签。 */
    private String statusLabel;

    /** 文件存储路径。 */
    private String filePath;

    /** 审核人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long verifiedBy;

    /** 审核时间。 */
    private LocalDateTime verifiedTime;

    /** 备注。 */
    private String remark;
}

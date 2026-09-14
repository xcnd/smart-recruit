package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Offer 下拉选项 VO（合同创建选择器使用）。
 *
 * @since 2026-04-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferOptionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String offerNo;
    private String candidateName;
    private String positionTitle;
    private Integer status;
    private String statusLabel;
}

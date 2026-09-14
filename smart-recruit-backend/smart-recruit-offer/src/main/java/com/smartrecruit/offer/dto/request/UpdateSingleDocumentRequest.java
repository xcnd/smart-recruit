package com.smartrecruit.offer.dto.request;

import lombok.Data;

/**
 * 更新单个入职文档请求。
 *
 * @since 2026-04-07
 */
@Data
public class UpdateSingleDocumentRequest {

    /** 文件存储路径，上传文件后状态自动流转为已提交。 */
    private String filePath;

    /** 文档状态编码。 */
    private Integer status;

    /** 文档备注。 */
    private String remark;
}

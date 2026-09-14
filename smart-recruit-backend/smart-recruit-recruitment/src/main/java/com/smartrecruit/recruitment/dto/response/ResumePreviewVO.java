package com.smartrecruit.recruitment.dto.response;

/**
 * 简历在线预览结果视图对象。
 *
 * <p>由 service 层产出，Controller 根据 {@code status} 构建对应的 HTTP 响应。
 * 成功时返回内容字节与 Content-Type；失败时返回错误信息。</p>
 *
 * @param status       HTTP 状态码（200/400/404/500）
 * @param content      成功时的响应内容字节
 * @param contentType 成功时的 Content-Type（失败时为 {@code null}）
 * @param errorMessage 失败时的错误信息（成功时为 {@code null}）
 * @since 2026-04-12
 */
public record ResumePreviewVO(
        int status,
        byte[] content,
        String contentType,
        String errorMessage
) {

    /** 简历不存在。 */
    public static ResumePreviewVO notFound() {
        return new ResumePreviewVO(404, null, null, null);
    }

    /** 请求不合法（文件缺失、下载失败等）。 */
    public static ResumePreviewVO badRequest(String message) {
        return new ResumePreviewVO(400, null, "text/plain", message);
    }

    /** 预览成功。 */
    public static ResumePreviewVO ok(byte[] content, String contentType) {
        return new ResumePreviewVO(200, content, contentType, null);
    }

    /** 服务端处理失败（.doc 转 HTML 异常等）。 */
    public static ResumePreviewVO serverError(String message) {
        return new ResumePreviewVO(500, null, "text/html", message);
    }
}

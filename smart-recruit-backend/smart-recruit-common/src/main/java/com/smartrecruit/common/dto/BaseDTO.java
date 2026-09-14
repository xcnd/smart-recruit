package com.smartrecruit.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础 DTO，包含所有领域 DTO 共享的通用审计字段。
 *
 * <p>使用 Java 25 灵活构造函数体，在字段赋值前进行验证。</p>
 *
 * @since 1.0.0
 */
public abstract sealed class BaseDTO implements Serializable
        permits BaseDTO.AuditableDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    public abstract Long getId();

    /**
     * 密封子类，包含用于追踪创建和更新信息的审计字段。
     */
    public static non-sealed class AuditableDTO extends BaseDTO {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 主键 ID。 */
        private Long id;

        /** 创建时间。 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        /** 更新时间。 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updateTime;

        /** 创建人 ID。 */
        private String createBy;

        /** 更新人 ID。 */
        private String updateBy;

        /** 逻辑删除标记：0 = 未删除，1 = 已删除。 */
        @JsonIgnore
        private Boolean deleted;

        public AuditableDTO() {}

        public AuditableDTO(Long id, LocalDateTime createTime, LocalDateTime updateTime,
                           String createBy, String updateBy) {
            this.id = id;
            this.createTime = createTime;
            this.updateTime = updateTime;
            this.createBy = createBy;
            this.updateBy = updateBy;
            this.deleted = false;
        }

        @Override
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }

        public String getCreateBy() {
            return createBy;
        }

        public void setCreateBy(String createBy) {
            this.createBy = createBy;
        }

        public String getUpdateBy() {
            return updateBy;
        }

        public void setUpdateBy(String updateBy) {
            this.updateBy = updateBy;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }
    }
}

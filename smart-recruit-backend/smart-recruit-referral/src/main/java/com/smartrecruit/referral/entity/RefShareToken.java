package com.smartrecruit.referral.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内推分享链接令牌实体，映射到 {@code ref_share_token} 表。
 *
 * <p>用于生成公开落地页的短链接令牌，避免在 URL 中出现中文或过长参数。</p>
 *
 * @author xdh
 * @since 2026-04-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ref_share_token")
public class RefShareToken implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分享令牌（12位随机字符串）。 */
    private String token;

    /** 对应的内推计划ID。 */
    private Long programId;

    /** 分享人姓名。 */
    private String referrerName;

    /** 分享来源渠道。 */
    private String source;

    /** 人类友好的内推码（6-8位大写字母+数字），用于页面展示和注册绑定。 */
    private String referralCode;

    /** 生成此分享令牌的员工用户ID（sys_user.id），用于追溯推荐人。 */
    private Long referrerId;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

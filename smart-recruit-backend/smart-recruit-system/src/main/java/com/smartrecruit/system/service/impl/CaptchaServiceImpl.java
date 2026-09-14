package com.smartrecruit.system.service.impl;

import com.smartrecruit.common.constant.Constants;
import com.smartrecruit.system.dto.response.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import com.smartrecruit.system.service.CaptchaService;

/**
 * 图片验证码服务（性能优化版）。
 *
 * <p>使用 Java Graphics2D 生成动态图片验证码。
 * 相比初版做了以下性能优化：
 * <ul>
 *   <li>用 {@link ThreadLocalRandom} 替代 {@link java.security.SecureRandom}，避免熵池阻塞</li>
 *   <li>缩小图片尺寸（110×40），减少像素填充量</li>
 *   <li>简化抗锯齿为基本级别，降低渲染开销</li>
 *   <li>减少干扰线（3 条）和噪点（12 个）</li>
 *   <li>收窄旋转角度（±8°），减少三角运算</li>
 *   <li>预置颜色池，避免反复创建 Color 对象</li>
 * </ul>
 *
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate redisTemplate;

    /** 排除易混淆字符：0/O/1/I/L */
    private static final String CHAR_SET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    /** 图片尺寸 */
    private static final int IMG_WIDTH = 110;
    private static final int IMG_HEIGHT = 40;

    /** 字体范围 */
    private static final int FONT_MIN_SIZE = 22;
    private static final int FONT_MAX_SIZE = 26;

    /** 干扰线 / 噪点数量（降低以提升生成速度） */
    private static final int INTERFERENCE_LINES = 3;
    private static final int NOISE_DOTS = 12;

    /** 预置深色（字符颜色池），减少 Color 对象创建 */
    private static final Color[] DARK_COLORS = {
            new Color(20, 40, 80), new Color(80, 20, 20), new Color(20, 80, 40),
            new Color(60, 20, 80), new Color(20, 80, 80), new Color(60, 30, 20),
    };

    /** 预置中间色（干扰线 / 噪点颜色池） */
    private static final Color[] MID_COLORS = {
            new Color(160, 160, 180), new Color(170, 150, 150), new Color(150, 170, 160),
            new Color(160, 150, 180),
    };

    /** 预置浅色背景 */
    private static final Color[] BG_COLORS = {
            new Color(242, 244, 248), new Color(246, 243, 240), new Color(240, 245, 243),
    };

    /** 复用字体对象 */
    private static final Font BASE_FONT = new Font("SansSerif", Font.BOLD, 22);

    /**
     * 生成验证码图片，存入 Redis 并返回响应。
     */
    public CaptchaVO generateCaptcha() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = generateRandomCode(Constants.CAPTCHA_CHAR_LENGTH);

        // 存入 Redis，设置过期时间
        String redisKey = Constants.REDIS_KEY_CAPTCHA + captchaId;
        redisTemplate.opsForValue().set(redisKey, code, Constants.CAPTCHA_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 生成图片
        String base64Image = generateBase64Image(code);

        log.debug("验证码生成成功: captchaId={}", captchaId);
        return new CaptchaVO(captchaId, base64Image);
    }

    /**
     * 校验验证码（一次性使用，校验后立即删除）。
     */
    public boolean validate(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            return false;
        }

        String redisKey = Constants.REDIS_KEY_CAPTCHA + captchaId;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        redisTemplate.delete(redisKey);

        if (storedCode == null) {
            log.debug("验证码不存在或已过期: captchaId={}", captchaId);
            return false;
        }

        boolean matched = storedCode.equalsIgnoreCase(captchaCode.trim());
        if (!matched) {
            log.debug("验证码错误: captchaId={}, expected={}, actual={}", captchaId, storedCode, captchaCode);
        }
        return matched;
    }

    // ================================================================
    // Private helpers
    // ================================================================

    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHAR_SET.charAt(ThreadLocalRandom.current().nextInt(CHAR_SET.length())));
        }
        return sb.toString();
    }

    private String generateBase64Image(String code) {
        BufferedImage image = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // 基本抗锯齿（LCD_HRGB 开销大，基本级别即可）
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBackground(g2d);
            drawInterferenceLines(g2d);
            drawCharacters(g2d, code);
            drawNoiseDots(g2d);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            throw new RuntimeException("验证码图片生成失败", e);
        } finally {
            g2d.dispose();
        }
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(BG_COLORS[ThreadLocalRandom.current().nextInt(BG_COLORS.length)]);
        g2d.fillRect(0, 0, IMG_WIDTH, IMG_HEIGHT);
    }

    private void drawInterferenceLines(Graphics2D g2d) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < INTERFERENCE_LINES; i++) {
            int x1 = rng.nextInt(IMG_WIDTH);
            int y1 = rng.nextInt(IMG_HEIGHT);
            int x2 = x1 + rng.nextInt(30) - 15;
            int y2 = y1 + rng.nextInt(24) - 12;
            g2d.setColor(MID_COLORS[rng.nextInt(MID_COLORS.length)]);
            g2d.setStroke(new BasicStroke(1.0f + rng.nextFloat()));
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawCharacters(Graphics2D g2d, String code) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int charCount = code.length();
        int totalCharWidth = (int) (IMG_WIDTH * 0.7);
        int startX = (IMG_WIDTH - totalCharWidth) / 2;

        for (int i = 0; i < charCount; i++) {
            String ch = String.valueOf(code.charAt(i));
            int fontSize = FONT_MIN_SIZE + rng.nextInt(FONT_MAX_SIZE - FONT_MIN_SIZE + 1);
            g2d.setFont(BASE_FONT.deriveFont((float) fontSize));
            g2d.setColor(DARK_COLORS[rng.nextInt(DARK_COLORS.length)]);

            int charWidth = g2d.getFontMetrics().charWidth(code.charAt(i));
            int x = startX + (i * totalCharWidth / charCount) + (totalCharWidth / charCount - charWidth) / 2;
            int y = IMG_HEIGHT / 2 + fontSize / 3 + rng.nextInt(4) - 2;

            // 旋转 ±8°（大幅减少三角函数运算）
            double angle = Math.toRadians(rng.nextInt(17) - 8);
            g2d.rotate(angle, x + charWidth / 2.0, y - fontSize / 4.0);
            g2d.drawString(ch, x, y);
            g2d.rotate(-angle, x + charWidth / 2.0, y - fontSize / 4.0);
        }
    }

    private void drawNoiseDots(Graphics2D g2d) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < NOISE_DOTS; i++) {
            g2d.setColor(MID_COLORS[rng.nextInt(MID_COLORS.length)]);
            g2d.fillRect(rng.nextInt(IMG_WIDTH), rng.nextInt(IMG_HEIGHT), 1, 1);
        }
    }
}
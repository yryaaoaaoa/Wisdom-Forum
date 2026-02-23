package com.yry.blog.myblogcommon.captcha.impl;

import com.yry.blog.myblogcommon.cache.manager.RedisCacheManager;
import com.yry.blog.myblogcommon.cache.util.CacheUtil;
import com.yry.blog.myblogcommon.captcha.CaptchaService;
import com.yry.blog.myblogcommon.captcha.dto.CaptchaResponse;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.yry.blog.myblogcommon.cache.enums.CacheTypeEnum.REDIS;

@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_LENGTH = 4;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 40;
    private final Random random = new Random();
    private final RedisCacheManager redisCacheManager;

    public CaptchaServiceImpl(RedisCacheManager redisCacheManager) {
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    public CaptchaResponse generateCaptcha() {
        String captchaText = generateCaptchaText();
        BufferedImage image = createCaptchaImage(captchaText);
        String imageBase64 = convertImageToBase64(image);
        CaptchaResponse response = new CaptchaResponse();
        String key = generateUniqueKey();
        response.setKey(key);
        response.setImageBase64(imageBase64);
        redisCacheManager.put("captcha:" + key, captchaText, 300,TimeUnit.SECONDS); // 设置验证码过期时间为300s，可修改
        return response;
    }

    @Override
    public boolean validateCaptcha(String key, String userInput) {
        // 1. 参数校验
        if (key == null || userInput == null) {
            return false;
        }

        // 2. 从缓存获取验证码文本
        String cachedCaptcha = (String) redisCacheManager.get("captcha:" + key);

        // 3. 验证码不存在或已过期
        if (cachedCaptcha == null) {
            return false;
        }

        // 4. 比对验证码（忽略大小写）
        boolean isValid = cachedCaptcha.equalsIgnoreCase(userInput);

        // 5. 不管验证成功与否都清除验证码（一次性使用）

        redisCacheManager.evict("captcha:" + key);
        return isValid;
    }

    // 下面都是生成验证码用到的工具类
    private String generateCaptchaText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CaptchaServiceImpl.DEFAULT_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private BufferedImage createCaptchaImage(String text) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.BLACK);

        int x = 20;
        for (char c : text.toCharArray()) {
            g2d.drawString(String.valueOf(c), x, 30);
            x += 20;
        }

        addNoiseLines(g2d);
        g2d.dispose();
        return image;
    }

    private void addNoiseLines(Graphics2D g2d) {
        g2d.setColor(Color.GRAY);
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(IMAGE_WIDTH);
            int y1 = random.nextInt(IMAGE_HEIGHT);
            int x2 = random.nextInt(IMAGE_WIDTH);
            int y2 = random.nextInt(IMAGE_HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    private String convertImageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("验证码图片转换失败", e);
        }
    }

    private String generateUniqueKey() {
        return "captcha_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
    }
}

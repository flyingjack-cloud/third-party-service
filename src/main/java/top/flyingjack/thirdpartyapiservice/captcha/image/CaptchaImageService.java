package top.flyingjack.thirdpartyapiservice.captcha.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.dto.CaptchaImgRes;
import top.flyingjack.common.error.exception.ServiceInternalException;
import top.flyingjack.thirdpartyapiservice.captcha.CaptchaUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * 验证码生成工具
 *
 * @author Zumin Li
 * @date 2025/4/13 0:32
 */
@Service
public class CaptchaImageService {
    private static final Logger log = LoggerFactory.getLogger(CaptchaImageService.class);

    public final static String IP_KEY = "ip";
    public final static String CAPTCHA_KEY = "captcha";

    // 图片大小
    private final static int WIDTH = 160;
    private final static int HEIGHT = 60;

    private CacheService cacheService;

    public CaptchaImageService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 生成base64验证码
     *
     * @param remoteIp 请求ip 用于后续验证
     */
    public CaptchaImgRes generateBase64Image(String remoteIp) {
        UUID uuid = UUID.randomUUID();
        String captchaText = CaptchaUtil.generateCaptchaText(6);
        String base64Img = generateBase64Captcha(captchaText);
        // 存入cache
        this.cacheService.hSet(CaptchaUtil.idToCacheKey(uuid), CaptchaImageService.CAPTCHA_KEY, captchaText);
        this.cacheService.hSet(CaptchaUtil.idToCacheKey(uuid), CaptchaImageService.IP_KEY, remoteIp);
        this.cacheService.expire(CaptchaUtil.idToCacheKey(uuid), 300L); // 有效期5分钟

        return new CaptchaImgRes(uuid, base64Img);
    }

    /**
     * 生成base64编码的图片
     *
     * @param captchaText 验证码，建议6位通过generateCaptchaText生成
     */
    private String generateBase64Captcha(String captchaText){
        BufferedImage captchaImage = generateCaptchaImage(captchaText);
        // 将图片转换为 Base64 编码
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(captchaImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.warn("Generate captcha failed - {}", e.getMessage());
            throw new ServiceInternalException("Generate captcha failed");
        }
    }


    private BufferedImage generateCaptchaImage(String captchaText) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置背景颜色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 设置字体
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.BLACK);

        // 绘制验证码文本
        g.drawString(captchaText, 20, 45);

        // 添加干扰线
        Random random = new Random();
        g.setColor(Color.GRAY);
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        g.dispose();
        return image;
    }
}


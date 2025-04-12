package top.flyingjack.thirdpartyapiservice.captcha;

import java.util.Random;
import java.util.UUID;

/**
 * Captcha工具类
 *
 * @author Zumin Li
 * @date 2025/4/16 18:58
 */
public class CaptchaUtil {
    public static final String CAPTCHA_CACHE_PREFIX = "captcha:";

    /**
     * 生成指定长度大小写验证码
     *
     * @param length 长度 建议6位
     */
    public static String generateCaptchaText(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789";
        return generateCaptchaText(length, chars);
    }

    /**
     * 生成指定长度大小写验证码
     *
     * @param length 长度 建议6位
     */
    public static String generateCaptchaNumber(int length) {
        String chars = "123456789";
        return generateCaptchaText(length, chars);
    }

    /**
     * 生成指定长度验证码
     *
     * @param length 长度 建议6位
     * @param base 生成基准
     */
    public static String generateCaptchaText(int length, String base) {
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < length; i++) {
            captcha.append(base.charAt(random.nextInt(base.length())));
        }
        return captcha.toString();
    }

    // 将验证码识别码转为redis key
    public static String idToCacheKey(String id){
        return CAPTCHA_CACHE_PREFIX + id;
    }

    public static String idToCacheKey(UUID id){
        return CAPTCHA_CACHE_PREFIX + id.toString();
    }
}

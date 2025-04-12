package top.flyingjack.thirdpartyapiservice.captcha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.dto.CaptchaRequest;
import top.flyingjack.thirdpartyapiservice.captcha.image.CaptchaImageService;

/**
 * 验证码相关服务
 *
 * @author Zumin Li
 * @date 2025/4/13 22:45
 */
@Service
public class CaptchaService {
    private CacheService cacheService;

    @Value("${captcha.verify.strict:true}")
    private boolean strictMode;

    public CaptchaService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 验证验证码是否正确, 验证通过时需要删除缓存
     *
     * @param captchaRequest 验证码请求体
     * @param remoteIp 请求ip
     * @return
     */
    public boolean verify(CaptchaRequest captchaRequest, String remoteIp) {
        String captchaInCache = (String) this.cacheService.hGet(CaptchaUtil.idToCacheKey(captchaRequest.captchaId()), CaptchaImageService.CAPTCHA_KEY);

        if (strictMode) {
            String ipInCache = (String) this.cacheService.hGet(CaptchaUtil.idToCacheKey(captchaRequest.captchaId()), CaptchaImageService.IP_KEY);
            // 严格模式必须保证请求ip统一
            if (ipInCache == null || !ipInCache.equals(remoteIp)) {
                return false;
            }
        }

        // 如果验证成功的话, 需要删除缓存
        if (captchaInCache != null && captchaInCache.equals(captchaRequest.captcha())){
            this.cacheService.del(CaptchaUtil.idToCacheKey(captchaRequest.captchaId()));
            return true;
        } else {
            return false;
        }
    }
}

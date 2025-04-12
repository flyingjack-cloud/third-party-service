package top.flyingjack.thirdpartyapiservice.captcha;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.dto.CaptchaRequest;
import top.flyingjack.thirdpartyapiservice.captcha.image.CaptchaImageService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
/**
 * 验证码生成，验证功能测试
 *
 * @author Zumin Li
 * @date 2025/4/14 0:36
 */
class CaptchaServiceTest {
    private CacheService cacheService = Mockito.mock(CacheService.class);
    private CaptchaService captchaService = new CaptchaService(cacheService);
    
    @Test
    public void should_verify_failed_when_captcha_is_incorrect() {
        // 非严格模式下次测试
        ReflectionTestUtils.setField(captchaService, "strictMode", false);
        UUID uuid = UUID.randomUUID();
        assertFalse(this.captchaService.verify(
                new CaptchaRequest(uuid.toString(), "aaaaa"),
                "8.8.8.8"
        ));
        Mockito.verify(cacheService, Mockito.times(1)).hGet(CaptchaUtil.idToCacheKey(uuid), CaptchaImageService.CAPTCHA_KEY);
        Mockito.verify(cacheService, Mockito.never()).hGet(uuid.toString(), CaptchaImageService.IP_KEY);

        // 严格模式下次测试
        ReflectionTestUtils.setField(captchaService, "strictMode", true);
        UUID uuidInStrict = UUID.randomUUID();
        assertFalse(this.captchaService.verify(
                new CaptchaRequest(uuidInStrict.toString(), "aaaaa"),
                "8.8.8.8"
        ));
        Mockito.verify(cacheService, Mockito.times(1)).hGet(CaptchaUtil.idToCacheKey(uuidInStrict), CaptchaImageService.CAPTCHA_KEY);
        Mockito.verify(cacheService, Mockito.times(1)).hGet(CaptchaUtil.idToCacheKey(uuidInStrict), CaptchaImageService.IP_KEY);
    }
}
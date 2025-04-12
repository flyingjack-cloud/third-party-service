package top.flyingjack.thirdpartyapiservice.captcha.image;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.dto.CaptchaImgRes;
import top.flyingjack.thirdpartyapiservice.captcha.CaptchaUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证码图片工具测试
 *
 * @author Zumin Li
 * @date 2025/4/14 1:14
 */
class CaptchaImageServiceTest {
    private CacheService cacheService = Mockito.mock(CacheService.class);
    private CaptchaImageService captchaImageService = new CaptchaImageService(cacheService);

    /**
     * 测试验证码生成，包括缓存储存过程
     */
    @Test
    public void should_get_captcha_text() throws IOException {
        CaptchaImgRes captchaImgRes = this.captchaImageService.generateBase64Image("8.8.8.8");
        assertNotNull(captchaImgRes.uuid());

        // 测试返回的image是否为base 64图片
        String base64Image = captchaImgRes.base64Image().contains(",") ? captchaImgRes.base64Image()
                .split(",")[1] : captchaImgRes.base64Image();
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(image);

        Mockito.verify(this.cacheService, Mockito.times(2)).hSet(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(this.cacheService, Mockito.times(1)).expire(Mockito.anyString(), Mockito.anyLong());
    }
}
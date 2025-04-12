package top.flyingjack.thirdpartyapiservice.captcha.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.thymeleaf.TemplateEngine;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.error.exception.BusinessException;
import top.flyingjack.common.error.exception.ThirdPartySystemException;
import top.flyingjack.thirdpartyapiservice.api.email.EmailService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Zumin Li
 * @date 2025/4/18 13:17
 */
class CaptchaEmailServiceTest {
    private final CacheService cacheService = Mockito.mock(CacheService.class);
    private final EmailService emailService = Mockito.mock(EmailService.class);
    private MessageSource messageSource = Mockito.mock(MessageSource.class);;
    private TemplateEngine templateEngine = Mockito.mock(TemplateEngine.class);;

    private CaptchaEmailService captchaEmailService;

    @BeforeEach
    void setUp() {
        Mockito.reset(cacheService);
        Mockito.reset(emailService);
        Mockito.reset(messageSource);
        Mockito.reset(templateEngine);

        this.captchaEmailService = new CaptchaEmailService(cacheService, emailService, messageSource, templateEngine);
    }

    @Test
    public void should_throw_error_with_invalid_email_or_bad_email_service() {
        assertThrows(IllegalArgumentException.class, () -> this.captchaEmailService
                .sendEmailCaptcha("notaemail", "8.8.8.1"));

        Mockito.when(this.emailService.sendMail(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new ThirdPartySystemException("test", "test", "test"));
        assertThrows(ThirdPartySystemException.class, () -> this.captchaEmailService
                .sendEmailCaptcha("test@test.com", "8.8.8.2"));
    }

    @Test
    public void should_trigger_attack_protect_with_too_many_send() {
        String testIp = "8.8.8.3";
        Mockito.when(this.emailService.sendMail(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
        this.captchaEmailService.sendEmailCaptcha("test@test.com", testIp);

        // 第一次要记录
        Mockito.verify(this.cacheService, Mockito.times(1)).set("CAPTCHA_EMAIL_SEND:" + testIp, "1");
        Mockito.verify(this.cacheService, Mockito.times(1)).expire("CAPTCHA_EMAIL_SEND:" + testIp, 30L);
        Mockito.when(this.cacheService.hasKey("CAPTCHA_EMAIL_SEND:" + testIp)).thenReturn(true);

        // 第二次返回业务错误
        assertThrows(BusinessException.class, () -> this.captchaEmailService.sendEmailCaptcha("test@test.com", testIp));
    }

    @Test
    public void should_send_email() {
        String testIp = "8.8.8.4";
        Mockito.when(this.emailService.sendMail(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(true);

        assertTrue(this.captchaEmailService.sendEmailCaptcha("test@test.com", testIp));
    }
}
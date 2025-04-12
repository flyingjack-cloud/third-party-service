package top.flyingjack.thirdpartyapiservice.captcha.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.exception.BusinessException;
import top.flyingjack.common.tool.MessageTool;
import top.flyingjack.common.tool.Verify;
import top.flyingjack.thirdpartyapiservice.api.email.EmailService;
import top.flyingjack.thirdpartyapiservice.captcha.CaptchaUtil;

import java.util.Locale;

/**
 * 验证码邮件发送
 *
 * @author Zumin Li
 * @date 2025/4/18 0:01
 */
@Service
public class CaptchaEmailService {
    private static final Logger log = LoggerFactory.getLogger(CaptchaEmailService.class);

    private final CacheService cacheService;
    private final EmailService emailService;
    private final MessageSource messageSource;
    private final TemplateEngine templateEngine;

    public CaptchaEmailService(CacheService cacheService, EmailService emailService, MessageSource messageSource,
                               TemplateEngine templateEngine) {
        this.cacheService = cacheService;
        this.emailService = emailService;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
    }

    /**
     * 发送验证码邮件
     *
     * @param email 收件人邮箱地址
     * @param remoteIp 请求ip.主要是用于攻击防护
     *
     */
    public boolean sendEmailCaptcha(String email, String remoteIp) throws RuntimeException {
        if (!Verify.verifyEmail(email)) {
            throw new IllegalArgumentException("Not a valid email");
        }

        migrateAttack(remoteIp);

        String code = CaptchaUtil.generateCaptchaText(6);

        Locale locale = LocaleContextHolder.getLocale();;
        String subject = MessageTool.getMessageByLocale(messageSource, "email.captcha.subject", locale);

        Context context = new Context(locale);
        context.setVariable("code", code);
        String emailContent = this.templateEngine.process("email_code.html", context);

        return this.emailService.sendMail(email, subject, emailContent);
    }

    // 防止刷接口，限制30秒才能发送一次
    private void migrateAttack(String remoteIp) {
        String key = "CAPTCHA_EMAIL_SEND:" + remoteIp;

        if (this.cacheService.hasKey(key)) {
            throw new BusinessException(ErrorCode.TO_MANY_LOGIN_ATTEMPT);
        } else {
            this.cacheService.set(key, "1");
            // 需要等待30秒才能再次发送
            this.cacheService.expire(key, 30L);
        }
    }
}

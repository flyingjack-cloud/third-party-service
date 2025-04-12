package top.flyingjack.thirdpartyapiservice.captcha.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.exception.BusinessException;
import top.flyingjack.common.error.exception.ServiceInternalException;
import top.flyingjack.common.error.exception.ThirdPartySystemException;
import top.flyingjack.common.tool.Verify;
import top.flyingjack.thirdpartyapiservice.api.sms.SmsService;
import top.flyingjack.thirdpartyapiservice.captcha.CaptchaUtil;
import top.flyingjack.thirdpartyapiservice.captcha.image.CaptchaImageService;

/**
 * @author Zumin Li
 * @date 2025/4/16 18:49
 */
@Service
public class CaptchaSmsService {
    private static final Logger log = LoggerFactory.getLogger(CaptchaSmsService.class);

    private final SmsService smsService;
    private final CacheService cacheService;

    @Value("${captcha.sms.sign-name}")
    private String signName;

    @Value("${captcha.sms.template-code}")
    private String templateCode;

    public CaptchaSmsService(CacheService cacheService, SmsService smsService) {
        this.cacheService = cacheService;
        this.smsService = smsService;
    }

    /**
     * 向指定手机号发送验证码，并且记录入缓存
     *
     * @param phone 手机号
     * @param remoteIp 请求ip

     * @throws ThirdPartySystemException 三方服务错误
     */
    public boolean sendSmsCaptcha(String phone, String remoteIp) throws RuntimeException {
        if (!Verify.verifyPhoneNumber(phone)){
            throw new IllegalArgumentException("Not a valid phone");
        }

        migrateAttack(remoteIp);

        String code = CaptchaUtil.generateCaptchaNumber(6);
        // 发送短信
        boolean isSuccess = this.smsService.sendSms(
                phone,
                String.format("{\"code\":\"%s\"}", code),
                signName,
                templateCode);

        if (isSuccess) {
            this.cacheService.hSet(CaptchaUtil.idToCacheKey(phone), CaptchaImageService.CAPTCHA_KEY, code);
            this.cacheService.hSet(CaptchaUtil.idToCacheKey(phone), CaptchaImageService.IP_KEY, remoteIp);
            this.cacheService.expire(CaptchaUtil.idToCacheKey(phone), 300L); // 有效期5分钟
            return true;
        } else {
            log.warn("Sms captcha send failed - to {} from {}", phone, remoteIp);
            throw new ServiceInternalException("Sms captcha send failed");
        }
    }

    // 防止刷接口，限制60秒才能发送一次
    private void migrateAttack(String remoteIp){
        String key = "CAPTCHA_SMS_SEND:" + remoteIp;

        if (this.cacheService.hasKey(key)){
            throw new BusinessException(ErrorCode.TO_MANY_LOGIN_ATTEMPT);
        } else {
            this.cacheService.set(key, "1");
            this.cacheService.expire(key, 60L);
        }
    }
}

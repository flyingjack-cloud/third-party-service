package top.flyingjack.thirdpartyapiservice.captcha.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.error.exception.BusinessException;
import top.flyingjack.common.error.exception.ServiceInternalException;
import top.flyingjack.common.error.exception.ThirdPartySystemException;
import top.flyingjack.thirdpartyapiservice.api.sms.SmsService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Zumin Li
 * @date 2025/4/17 0:08
 */

class CaptchaSmsServiceTest {
    private SmsService smsService = Mockito.mock(SmsService.class);
    private CacheService cacheService = Mockito.mock(CacheService.class);

    private CaptchaSmsService captchaSmsService = new CaptchaSmsService(cacheService, smsService);

    @BeforeEach
    void setUp() {
        Mockito.reset(smsService);
        Mockito.reset(cacheService);

        ReflectionTestUtils.setField(captchaSmsService, "signName", "signName");
        ReflectionTestUtils.setField(captchaSmsService, "templateCode", "templateCode");
    }

    @Test
    public void should_throw_error_with_invalid_phone_or_bad_sms_service() {
        assertThrows(IllegalArgumentException.class, () -> this.captchaSmsService
                .sendSmsCaptcha("notaphone", "8.8.8.1"));

        Mockito.when(this.smsService.sendSms(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenThrow(new ThirdPartySystemException("test", "test", "test"));

        assertThrows(ThirdPartySystemException.class, () -> this.captchaSmsService
                .sendSmsCaptcha("13012341234", "8.8.8.2"));
    }

    @Test
    public void should_trigger_attack_protect_with_too_many_send() {
        String testIp = "8.8.8.3";
        Mockito.when(this.smsService.sendSms(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(true);
        this.captchaSmsService.sendSmsCaptcha("13012341234", testIp);
        // 第一次要记录
        Mockito.verify(this.cacheService, Mockito.times(1)).set("CAPTCHA_SMS_SEND:" + testIp, "1");
        Mockito.verify(this.cacheService, Mockito.times(1)).expire("CAPTCHA_SMS_SEND:" + testIp, 60L);
        Mockito.when(this.cacheService.hasKey("CAPTCHA_SMS_SEND:" + testIp)).thenReturn(true);

        // 第二次返回业务错误
        assertThrows(BusinessException.class, () -> this.captchaSmsService.sendSmsCaptcha("13012341234", testIp));
    }

    @Test
    public void should_get_result_with_valid_request() {
        Mockito.when(this.smsService.sendSms(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(true);
        assertTrue(this.captchaSmsService.sendSmsCaptcha("13012341234", "8.8.8.6"));
        Mockito.verify(this.cacheService, Mockito.times(2)).hSet(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(this.cacheService, Mockito.atLeastOnce()).expire(Mockito.anyString(), Mockito.anyLong());

        Mockito.when(this.smsService.sendSms(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(false);
        assertThrows(ServiceInternalException.class, () -> this.captchaSmsService
                .sendSmsCaptcha("13012341234", "8.8.8.9"));
    }
}
package top.flyingjack.thirdpartyapiservice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import top.flyingjack.thirdpartyapiservice.api.sms.AliSmsServiceImpl;
import top.flyingjack.thirdpartyapiservice.api.sms.SmsService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 阿里云接口测试, 只允许用于手动检查接口情况 - 注意测试会产生费用，不允许托管测试，只允许手动点击测试
 *
 * @author Zumin Li
 * @date 2025/4/17 0:36
 */
class AliSmsServiceTest {
    private static final Logger log = LoggerFactory.getLogger(AliSmsServiceTest.class);
    // 防止误测试
    private final boolean TEST_SWITCH = false;

    // 测试时手动设置这里的访问id和secret, 测试完以后再删除
    private String accessId;
    private String accessSecret;
    private final String testSignName = "阿里云短信测试";
    private final String testTemplateCode = "SMS_154950909";
    private final String testTemplateParam = "{\"code\":\"1234\"}";
    // 测试手机需要提前在服务商加入名单
    private String testPhone;

    //手动测试时代开
    // @Test
    public void DO_NOT_AUTO_TEST() {
        // 只有打开开关才允许测试
        if (TEST_SWITCH && accessId != null && accessSecret != null && testPhone != null) {
            log.error("YOU TRIGGER SMS TEST, ITS NOT FREE!");

            SmsService service = new AliSmsServiceImpl();
            ReflectionTestUtils.setField(service, "accessId", accessId);
            ReflectionTestUtils.setField(service, "accessSecret", accessSecret);

            boolean res = service.sendSms(
                    testPhone,
                    testTemplateParam,
                    testSignName,
                    testTemplateCode
            );

            assertTrue(res);
        }
    }
}
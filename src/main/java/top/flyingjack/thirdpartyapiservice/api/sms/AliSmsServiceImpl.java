package top.flyingjack.thirdpartyapiservice.api.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.flyingjack.common.error.exception.ThirdPartySystemException;

/**
 * 阿里云短信接口
 *
 * @author Zumin Li
 * @date 2025/4/16 17:15
 */
@Service
public class AliSmsServiceImpl implements SmsService{
    private static final Logger logger = LoggerFactory.getLogger(AliSmsServiceImpl.class);

    @Value("${aliyun.sms.access.id}")
    private String accessId;

    @Value("${aliyun.sms.access.secret}")
    private String accessSecret;

    /**
     * 使用AK&SK初始化账号Client
     * @return Client
     */
    public Client createClient() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(accessId)
                .setAccessKeySecret(accessSecret);
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new Client(config);
    }

    /**
     * 发送验证码
     *
     * @param phoneNumber 发送对象手机号码
     * @param templateParam 模板参数，需要json化
     * @param signName 签名名称
     * @param templateCode 模板code
     */
    @Override
    public boolean sendSms(String phoneNumber, String templateParam, String signName, String templateCode) throws ThirdPartySystemException{
        Client client;

        try {
            client = this.createClient();
        } catch (Exception e){
            logger.error("Aliyun sms service connected failed - {}", e.getMessage());
            throw new ThirdPartySystemException("Sms service connected failed", "Aliyun", "sms");
        }

        SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setPhoneNumbers(phoneNumber)
                .setTemplateParam(templateParam);

        RuntimeOptions runtime = new RuntimeOptions();

        try {
            SendSmsResponse response = client.sendSmsWithOptions(sendSmsRequest, runtime);
            logger.info("Aliyun sms service response - {}", response.getBody().getMessage());
            return response.statusCode == 200;
        }  catch (Exception error) {
            logger.error("Send sms by aliyun failed - {}", error.getMessage());
            throw new ThirdPartySystemException("Send sms failed ", "Aliyun", "sms");
        }
    }
}


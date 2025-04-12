package top.flyingjack.thirdpartyapiservice.api.sms;

import top.flyingjack.common.error.exception.ThirdPartySystemException;

/**
 * @author Zumin Li
 * @date 2025/4/16 17:22
 */
public interface SmsService {
    /**
     * 发送验证码
     *
     * @param phoneNumber 发送对象手机号码
     * @param templateParam 模板参数，需要json化
     * @param signName 签名名称
     * @param templateCode 模板code
     *
     * @return boolean 是否发送成功
     * @throws Exception 短信服务错误
     */
    boolean sendSms(String phoneNumber, String templateParam, String signName, String templateCode) throws ThirdPartySystemException;
}

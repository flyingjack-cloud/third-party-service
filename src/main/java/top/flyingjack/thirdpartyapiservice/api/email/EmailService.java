package top.flyingjack.thirdpartyapiservice.api.email;

import top.flyingjack.common.error.exception.ThirdPartySystemException;

/**
 * 邮件服务
 *
 * @author Zumin Li
 * @date 2025/4/17 23:01
 */
public interface EmailService {
    /**
     *
     * @param to 接受地址
     * @param subject 主题
     * @param content 内容，可以为html
     *
     * @throws ThirdPartySystemException 可能抛出三方错误
     */
     boolean sendMail(String to, String subject, String content) throws ThirdPartySystemException;
}

package top.flyingjack.thirdpartyapiservice.api.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import top.flyingjack.common.error.exception.ThirdPartySystemException;

/**
 * 网易邮箱发送实现
 *
 * @author Zumin Li
 * @date 2025/4/17 23:03
 */
@Service
public class NeteaseEmailServiceImpl implements EmailService{
    private static final Logger log = LoggerFactory.getLogger(NeteaseEmailServiceImpl.class);

    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public NeteaseEmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean sendMail(String to, String subject, String content) throws ThirdPartySystemException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            log.error("Send email failed：{}", e.getMessage());
            throw new ThirdPartySystemException(e.getMessage(), "EMAIL", "netease");
        }
        return true;
    }
}

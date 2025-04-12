package top.flyingjack.thirdpartyapiservice.api.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

/**
 * 网易邮箱发送测试 - 会真实发送
 *
 * @author Zumin Li
 * @date 2025/4/17 23:22
 */
class NeteaseEmailServiceImplTest {
    private static Properties properties;

    private String testAddress;
    private JavaMailSender mailSender;
    private EmailService emailService;

    @BeforeAll
    static void init(){
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application-test.yml"));
        properties = yamlFactory.getObject();
    }

    @BeforeEach
    void setUp(){
        this.mailSender = new JavaMailSenderImpl();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 配置邮件服务器
        mailSender.setHost(properties.getProperty("spring.mail.host")); // 设置SMTP服务器地址
        mailSender.setUsername(properties.getProperty("spring.mail.username")); // 设置用户名
        mailSender.setPassword(properties.getProperty("spring.mail.password")); // 设置密码

        // 配置属性
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp"); // 使用SMTP协议
        props.put("mail.smtp.auth", "true"); // 启用认证
        props.put("mail.smtp.starttls.enable", "true"); // 启用STARTTLS
        props.put("mail.smtp.starttls.required", "true"); // 启用STARTTLS
        props.put("mail.debug", "true"); // 启用调试模式（测试环境可用）

        this.emailService = new NeteaseEmailServiceImpl(mailSender);
        this.testAddress = properties.getProperty("test.email.to");

        ReflectionTestUtils.setField(this.emailService,"from" , properties.getProperty("spring.mail.username"));
    }

    @Test
    public void should_send_email(){
        String subject = "Test Attachment";
        String text = "See attachment";
        Assertions.assertTrue(this.emailService.sendMail(testAddress, subject, text));
    }
}
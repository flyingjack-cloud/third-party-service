package top.flyingjack.thirdpartyapiservice.captcha;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.CaptchaImgRes;
import top.flyingjack.common.dto.CaptchaRequest;
import top.flyingjack.common.tool.HttpTools;
import top.flyingjack.thirdpartyapiservice.captcha.email.CaptchaEmailService;
import top.flyingjack.thirdpartyapiservice.captcha.image.CaptchaImageService;
import top.flyingjack.thirdpartyapiservice.captcha.sms.CaptchaSmsService;

/**
 * 验证码生成接口
 *
 * @author Zumin Li
 * @date 2025/4/13 16:21
 */
@RestController
@RequestMapping("/captcha")
@Tag(name = "验证码接口", description = "本地生成验证码")
public class CaptchaController {
    private final CaptchaImageService captchaImageService;
    private final CaptchaSmsService captchaSmsService;
    private final CaptchaEmailService captchaEmailService;

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaImageService captchaImageService, CaptchaSmsService captchaSmsService,
                             CaptchaEmailService captchaEmailService, CaptchaService captchaService) {
        this.captchaImageService = captchaImageService;
        this.captchaSmsService = captchaSmsService;
        this.captchaEmailService = captchaEmailService;
        this.captchaService = captchaService;
    }

    @Operation(summary = "生成验证用uuid和base64的验证码图片(对外接口，需要gateway中转发)")
    @GetMapping("/generate/image")
    public ResponseEntity<ApiRes<CaptchaImgRes>> generateImageCaptcha(HttpServletRequest request) {
        return ResponseEntity.ok(
                ApiRes.success(captchaImageService.generateBase64Image(HttpTools.getClientIp(request)))
        );
    }

    @Operation(summary = "向手机号发送验证码, 会产生费用，不要随意调用！！！")
    @GetMapping("/generate/sms")
    public ResponseEntity<ApiRes<?>> generatePhoneCaptcha(HttpServletRequest request, @RequestParam String phone) {
        return ResponseEntity.ok(
                ApiRes.success(captchaSmsService.sendSmsCaptcha(phone, HttpTools.getClientIp(request)))
        );
    }

    @Operation(summary = "向邮箱发送验证码")
    @GetMapping("/generate/mail")
    public ResponseEntity<ApiRes<?>> generateEmailCaptcha(HttpServletRequest request, @RequestParam String email) {
        return ResponseEntity.ok(
                ApiRes.success(captchaEmailService.sendEmailCaptcha(email, HttpTools.getClientIp(request)))
        );
    }

    @Operation(summary = "验证传入的id和验证码，严格模式下也会验证ip - 内部服务调用")
    @PostMapping("/verify")
    public boolean verify(@RequestBody CaptchaRequest captchaRequest,
                          HttpServletRequest request) {
        return captchaService.verify(captchaRequest, HttpTools.getClientIp(request));
    }
}

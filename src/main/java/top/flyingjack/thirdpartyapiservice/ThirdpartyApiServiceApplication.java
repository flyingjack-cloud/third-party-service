package top.flyingjack.thirdpartyapiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import top.flyingjack.common.config.anotation.EnableGlobalCache;
import top.flyingjack.common.config.anotation.EnableGlobalException;
import top.flyingjack.common.config.anotation.EnableGlobalI18n;

@EnableGlobalException
@EnableGlobalI18n
@EnableGlobalCache
@SpringBootApplication
public class ThirdpartyApiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThirdpartyApiServiceApplication.class, args);
    }
}

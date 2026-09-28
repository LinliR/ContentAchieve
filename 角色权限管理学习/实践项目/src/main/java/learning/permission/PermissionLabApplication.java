package learning.permission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PermissionLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(PermissionLabApplication.class, args);
        org.slf4j.LoggerFactory.getLogger(PermissionLabApplication.class)
            .info("学习项目已就绪：数据初始化完成，可以开始登录练习。");
    }
}

package com.yry.blog;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync // <--- 添加此注解
@SpringBootApplication
@EnableScheduling // 开启定时任务
public class MyblogServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyblogServerApplication.class, args);
    }

}

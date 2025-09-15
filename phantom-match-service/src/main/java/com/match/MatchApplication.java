package com.match;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.match",        // 默认的扫描范围
        "com.game.common"  // 添加common模块的扫描范围
})
@EnableDiscoveryClient
public class MatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchApplication.class,args);
    }
}
package com.atguigu.gmall.index;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;

@SpringBootApplication
@EnableFeignClients
public class GmallIndexApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallIndexApplication.class, args);
    }

}

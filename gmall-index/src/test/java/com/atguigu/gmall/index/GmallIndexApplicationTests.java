package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void contextLoads() {

        this.redisTemplate.opsForValue().set("lock","123456");
    }
    @Test
    public void test(){
        Object[] objects = new Object[]{1,2,3};
        System.out.println(objects);
    }

}

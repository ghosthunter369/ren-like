package org.stefanie.renlike;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OjBackendQuestionServiceApplicationTests {

    @Test
    void contextLoads() {
    }
    @Test
    public void testBloom(){
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://192.168.101.128:6379")
                .setDatabase(10);
        // 设置 JSON 序列化
        //TODO
        config.setCodec(new JsonJacksonCodec());
        RedissonClient redissonClient = Redisson.create(config);
        //先经过布隆过滤器过滤
        RBloomFilter<Object> questionIdBloomFilter = redissonClient.getBloomFilter("blog:bloom:filter");
        questionIdBloomFilter.tryInit(1000, 0.01);
    }

}
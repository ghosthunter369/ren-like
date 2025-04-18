package org.stefanie.renlike.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public  RedissonClient createClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://192.168.101.128:6379")
                .setDatabase(10);
        // 设置 JSON 序列化
        //TODO
        config.setCodec(new JsonJacksonCodec());

        return Redisson.create(config);
    }
}

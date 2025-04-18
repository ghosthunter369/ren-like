package org.stefanie.renlike;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.stefanie.renlike.mapper")
public class RenLikeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RenLikeApplication.class, args);
    }

}

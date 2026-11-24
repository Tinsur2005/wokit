package cn.tinsur.wokit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.tinsur.wokit.mapper")
public class WokitApplication {

    public static void main(String[] args) {
        SpringApplication.run(WokitApplication.class, args);
    }

}

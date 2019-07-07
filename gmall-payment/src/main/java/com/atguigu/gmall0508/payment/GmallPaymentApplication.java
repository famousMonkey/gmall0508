package com.atguigu.gmall0508.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.atguigu.gmall0508")
@MapperScan(basePackages = "com.atguigu.gmall0508.payment.mapper")
public class GmallPaymentApplication {

	public static void main(String[] args) {

		SpringApplication.run(GmallPaymentApplication.class, args);
	}
}

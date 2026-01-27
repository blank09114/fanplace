package kr.co.fanplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FanplaceApplication
{ public static void main(String[] args) {
		SpringApplication.run(FanplaceApplication.class, args);
	} }
package com.agenticai.adk.app;
/** @author lalamanil **/
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class AdkApplication {
	public static void main(String[] args) {
		SpringApplication.run(AdkApplication.class, args);
	}
}

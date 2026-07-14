package com.example.ovikBot.OvikBot;

import com.example.ovikBot.OvikBot.config.AdminBootstrapProperties;
import com.example.ovikBot.OvikBot.config.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ AuthProperties.class, AdminBootstrapProperties.class })
public class OvikBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(OvikBotApplication.class, args);
	}

}
package com.yantai.superinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SuperinventoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuperinventoryApplication.class, args);
	}

}

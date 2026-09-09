package com.ryanbondoc.fintech.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.ryanbondoc.fintech.transaction.client.impl.AccountServiceProperties;

@SpringBootApplication
@EnableConfigurationProperties(AccountServiceProperties.class)
public class TransactionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionServiceApplication.class, args);
	}

}

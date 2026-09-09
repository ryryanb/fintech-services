package com.ryanbondoc.fintech.transaction.client.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "services.account")
public record AccountServiceProperties(
String baseUrl
) {
}
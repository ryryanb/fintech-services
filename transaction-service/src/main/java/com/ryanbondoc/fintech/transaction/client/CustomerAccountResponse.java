package com.ryanbondoc.fintech.transaction.client;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal account projection returned by Account Service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerAccountResponse(UUID id) {
}

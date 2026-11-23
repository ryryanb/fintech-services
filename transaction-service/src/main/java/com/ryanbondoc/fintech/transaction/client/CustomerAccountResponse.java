package com.ryanbondoc.fintech.transaction.client;

import java.util.UUID;

/** Minimal account projection returned by Account Service. */
public record CustomerAccountResponse(UUID id) {
}

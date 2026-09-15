package com.ryanbondoc.fintech.transaction.client;

import java.util.UUID;

public record AccountOwnershipResponse(
        UUID id,
        UUID customerId) {
}
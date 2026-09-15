package com.ryanbondoc.fintech.transaction.client;

import java.util.UUID;

public interface AccountServiceClient {

    boolean accountExists(UUID accountId, String bearerToken);

    AccountOwnershipResponse getAccountOwnership(
            UUID accountId,
            String bearerToken);

}

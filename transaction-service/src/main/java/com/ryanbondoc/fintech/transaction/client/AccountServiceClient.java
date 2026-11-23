package com.ryanbondoc.fintech.transaction.client;

import java.util.List;
import java.util.UUID;

public interface AccountServiceClient {

    boolean accountExists(UUID accountId, String bearerToken);

    AccountOwnershipResponse getAccountOwnership(UUID accountId, String bearerToken);

    List<CustomerAccountResponse> getCustomerAccounts(String bearerToken);
}

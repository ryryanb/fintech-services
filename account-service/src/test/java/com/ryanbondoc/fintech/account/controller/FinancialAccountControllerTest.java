package com.ryanbondoc.fintech.account.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.enums.AccountStatus;
import com.ryanbondoc.fintech.account.enums.AccountType;
import com.ryanbondoc.fintech.account.security.AccountAuthorizationService;
import com.ryanbondoc.fintech.account.service.FinancialAccountService;

@WebMvcTest(FinancialAccountController.class)
@ImportAutoConfiguration(exclude = {
                OAuth2ResourceServerAutoConfiguration.class,
                OAuth2ResourceServerWebSecurityAutoConfiguration.class
})
class FinancialAccountControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private FinancialAccountService financialAccountService;

        @MockitoBean
        private AccountAuthorizationService accountAuthorizationService;

        @Test
        void shouldGetAccountById() throws Exception {

                UUID accountId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();

                FinancialAccountResponse response = new FinancialAccountResponse(
                                accountId,
                                customerId,
                                "Primary Account",
                                AccountType.BANK_ACCOUNT,
                                "PHP",
                                new BigDecimal("10000.00"),
                                "FinTech Bank",
                                AccountStatus.ACTIVE);

                when(financialAccountService.getAccount(accountId))
                                .thenReturn(response);

                mockMvc.perform(
                                get("/accounts/{accountId}", accountId))
                                .andExpect(status().isOk())
                                .andExpect(
                                                jsonPath("$.id")
                                                                .value(accountId.toString()))
                                .andExpect(
                                                jsonPath("$.customerId")
                                                                .value(customerId.toString()))
                                .andExpect(
                                                jsonPath("$.currency")
                                                                .value("PHP"));
        }
}

package com.ryanbondoc.fintech.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ryanbondoc.fintech.transaction.dto.TransactionRequest;
import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;
import com.ryanbondoc.fintech.transaction.exception.TransactionNotFoundException;
import com.ryanbondoc.fintech.transaction.service.FinancialTransactionService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(FinancialTransactionController.class)
class FinancialTransactionControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private FinancialTransactionService transactionService;

        @Test
        void shouldCreateTransaction() throws Exception {

                UUID transactionId = UUID.randomUUID();
                UUID accountId = UUID.randomUUID();

                OffsetDateTime transactionDate = OffsetDateTime.parse("2026-09-08T14:30:00Z");

                TransactionRequest request = new TransactionRequest(
                                accountId,
                                TransactionType.PAYMENT,
                                TransactionDirection.DEBIT,
                                new BigDecimal("1250.00"),
                                "PHP",

                                "Utility payment",
                                "merchant",
                                TransactionCategory.FEES,
                                transactionDate);

                TransactionResponse response = new TransactionResponse(
                                transactionId,
                                accountId,
                                TransactionType.PAYMENT,
                                TransactionDirection.DEBIT,
                                new BigDecimal("1250.00"),
                                "PHP",
                                "merchant",
                                TransactionCategory.FEES,
                                "Utility payment",
                                TransactionStatus.COMPLETED,
                                transactionDate);

                when(transactionService.createTransaction(any(TransactionRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(post("/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                                .andExpect(jsonPath("$.type").value("PAYMENT"))
                                .andExpect(jsonPath("$.direction").value("DEBIT"))
                                .andExpect(jsonPath("$.amount").value(1250.00))
                                .andExpect(jsonPath("$.currency").value("PHP"))
                                .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        void shouldRejectZeroAmount() throws Exception {

                UUID accountId = UUID.randomUUID();

                TransactionRequest request = new TransactionRequest(
                                accountId,
                                TransactionType.PAYMENT,
                                TransactionDirection.DEBIT,
                                BigDecimal.ZERO,
                                "PHP",
                                "Invalid payment",
                                "merchant",
                                TransactionCategory.FEES,
                                null);

                mockMvc.perform(post("/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void shouldGetTransactionById() throws Exception {

                UUID transactionId = UUID.randomUUID();
                UUID accountId = UUID.randomUUID();

                TransactionResponse response = new TransactionResponse(
                                transactionId,
                                accountId,
                                TransactionType.PAYMENT,
                                TransactionDirection.DEBIT,
                                new BigDecimal("1250.00"),
                                "PHP",
                                "SM Supermarket",
                                TransactionCategory.GROCERIES,
                                "Weekly groceries",
                                TransactionStatus.COMPLETED,
                                OffsetDateTime.parse("2026-09-09T14:30:00Z"));

                when(transactionService.getTransaction(transactionId))
                                .thenReturn(response);

                mockMvc.perform(
                                get("/transactions/{transactionId}", transactionId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(transactionId.toString()))
                                .andExpect(jsonPath("$.accountId")
                                                .value(accountId.toString()))
                                .andExpect(jsonPath("$.merchant")
                                                .value("SM Supermarket"))
                                .andExpect(jsonPath("$.amount")
                                                .value(1250.00))
                                .andExpect(jsonPath("$.currency")
                                                .value("PHP"))
                                .andExpect(jsonPath("$.category")
                                                .value("GROCERIES"))
                                .andExpect(jsonPath("$.description")
                                                .value("Weekly groceries"))
                                .andExpect(jsonPath("$.status")
                                                .value("COMPLETED"));
        }

        @Test
        void shouldReturnNotFoundWhenTransactionDoesNotExist()
                        throws Exception {

                UUID transactionId = UUID.randomUUID();

                when(transactionService.getTransaction(transactionId))
                                .thenThrow(
                                                new TransactionNotFoundException(transactionId));

                mockMvc.perform(
                                get("/transactions/{transactionId}", transactionId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error")
                                                .value("TRANSACTION_NOT_FOUND"));
        }

}

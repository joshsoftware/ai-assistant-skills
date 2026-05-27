package com.joshsoftware.mockbank.controller;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.constants.TransactionConstants;
import com.joshsoftware.mockbank.dto.ApiResponse;
import com.joshsoftware.mockbank.dto.response.TransactionResponseDTO;
import com.joshsoftware.mockbank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> findById(@PathVariable UUID id) {
        log.info("[TransactionController#findById] ENTRY - transactionId={}", id);
        TransactionResponseDTO response = transactionService.findById(id);
        log.info("[TransactionController#findById] EXIT  - transactionId={}", id);
        return ResponseEntity.ok(ApiResponse.success(TransactionConstants.TRANSACTION_FETCHED, response));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDTO>>> findByAccountId(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("[TransactionController#findByAccountId] ENTRY - accountId={}", accountId);
        Page<TransactionResponseDTO> page = transactionService.findByAccountId(accountId, pageable);
        log.info("[TransactionController#findByAccountId] EXIT  - totalElements={}", page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(TransactionConstants.TRANSACTIONS_FETCHED, page));
    }
}

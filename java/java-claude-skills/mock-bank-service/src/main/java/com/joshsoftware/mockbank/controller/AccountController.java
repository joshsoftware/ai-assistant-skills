package com.joshsoftware.mockbank.controller;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.constants.AccountConstants;
import com.joshsoftware.mockbank.dto.ApiResponse;
import com.joshsoftware.mockbank.dto.request.CreateAccountRequestDTO;
import com.joshsoftware.mockbank.dto.request.UpdateAccountStatusRequestDTO;
import com.joshsoftware.mockbank.dto.response.AccountResponseDTO;
import com.joshsoftware.mockbank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponseDTO>> create(
            @Valid @RequestBody CreateAccountRequestDTO request) {

        log.info("[AccountController#create] ENTRY - customerId={}", request.getCustomerId());
        AccountResponseDTO response = accountService.create(request);
        log.info("[AccountController#create] EXIT  - accountId={}", response.getId());
        return ResponseEntity.ok(ApiResponse.success(AccountConstants.ACCOUNT_CREATED, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> findById(@PathVariable UUID id) {
        log.info("[AccountController#findById] ENTRY - accountId={}", id);
        AccountResponseDTO response = accountService.findById(id);
        log.info("[AccountController#findById] EXIT  - accountId={}", id);
        return ResponseEntity.ok(ApiResponse.success(AccountConstants.ACCOUNT_FETCHED, response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<AccountResponseDTO>>> findByCustomerId(
            @PathVariable UUID customerId) {

        log.info("[AccountController#findByCustomerId] ENTRY - customerId={}", customerId);
        List<AccountResponseDTO> accounts = accountService.findByCustomerId(customerId);
        log.info("[AccountController#findByCustomerId] EXIT  - count={}", accounts.size());
        return ResponseEntity.ok(ApiResponse.success(AccountConstants.ACCOUNTS_FETCHED, accounts));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AccountResponseDTO>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequestDTO request) {

        log.info("[AccountController#updateStatus] ENTRY - accountId={}, status={}", id, request.getStatus());
        AccountResponseDTO response = accountService.updateStatus(id, request);
        log.info("[AccountController#updateStatus] EXIT  - accountId={}", id);
        return ResponseEntity.ok(ApiResponse.success(AccountConstants.ACCOUNT_STATUS_UPDATED, response));
    }
}

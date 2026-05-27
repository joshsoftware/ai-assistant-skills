package com.joshsoftware.mockbank.controller;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.constants.PaymentConstants;
import com.joshsoftware.mockbank.dto.ApiResponse;
import com.joshsoftware.mockbank.dto.request.InitiatePaymentRequestDTO;
import com.joshsoftware.mockbank.dto.response.PaymentResponseDTO;
import com.joshsoftware.mockbank.service.PaymentService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> initiate(
            @Valid @RequestBody InitiatePaymentRequestDTO request) {

        log.info("[PaymentController#initiate] ENTRY - from={}, to={}, amount={}", request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        PaymentResponseDTO response = paymentService.initiate(request);
        log.info("[PaymentController#initiate] EXIT  - paymentId={}", response.getId());
        return ResponseEntity.ok(ApiResponse.success(PaymentConstants.PAYMENT_INITIATED, response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponseDTO>> findById(@PathVariable UUID id) {
        log.info("[PaymentController#findById] ENTRY - paymentId={}", id);
        PaymentResponseDTO response = paymentService.findById(id);
        log.info("[PaymentController#findById] EXIT  - paymentId={}", id);
        return ResponseEntity.ok(ApiResponse.success(PaymentConstants.PAYMENT_FETCHED, response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponseDTO>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("[PaymentController#findAll] ENTRY - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<PaymentResponseDTO> page = paymentService.findAll(pageable);
        log.info("[PaymentController#findAll] EXIT  - totalElements={}", page.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(PaymentConstants.PAYMENTS_FETCHED, page));
    }
}

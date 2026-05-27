package com.joshsoftware.mockbank.service;

import com.joshsoftware.mockbank.dto.request.InitiatePaymentRequestDTO;
import com.joshsoftware.mockbank.dto.response.PaymentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    // Initiates a payment — debits fromAccount, credits toAccount, creates a Transaction and Payment record.
    PaymentResponseDTO initiate(InitiatePaymentRequestDTO request);

    // Retrieves a payment by its UUID.
    PaymentResponseDTO findById(UUID id);

    // Returns a paginated list of all payments.
    Page<PaymentResponseDTO> findAll(Pageable pageable);
}

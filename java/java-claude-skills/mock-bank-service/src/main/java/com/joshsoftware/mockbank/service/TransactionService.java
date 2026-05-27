package com.joshsoftware.mockbank.service;

import com.joshsoftware.mockbank.dto.response.TransactionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionService {

    // Retrieves a single transaction by its UUID.
    TransactionResponseDTO findById(UUID id);

    // Returns a paginated list of all transactions for the given account.
    Page<TransactionResponseDTO> findByAccountId(UUID accountId, Pageable pageable);
}

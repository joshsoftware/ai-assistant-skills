package com.joshsoftware.mockbank.service.impl;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.dto.response.TransactionResponseDTO;
import com.joshsoftware.mockbank.exception.ResourceNotFoundException;
import com.joshsoftware.mockbank.mapper.TransactionMapper;
import com.joshsoftware.mockbank.repository.AccountRepository;
import com.joshsoftware.mockbank.repository.TransactionRepository;
import com.joshsoftware.mockbank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository     accountRepository;
    private final TransactionMapper     transactionMapper;

    // Retrieves a transaction by its UUID.
    @Override
    public TransactionResponseDTO findById(UUID id) {
        log.debug("[TransactionServiceImpl#findById] ENTRY - transactionId={}", id);

        TransactionResponseDTO response = transactionRepository.findById(id)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        log.debug("[TransactionServiceImpl#findById] EXIT - transactionId={}", id);
        return response;
    }

    // Returns paginated transactions for a given account (as sender or receiver).
    @Override
    public Page<TransactionResponseDTO> findByAccountId(UUID accountId, Pageable pageable) {
        log.debug("[TransactionServiceImpl#findByAccountId] ENTRY - accountId={}", accountId);

        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account", "id", accountId);
        }

        Page<TransactionResponseDTO> page = transactionRepository
                .findByFromAccountIdOrToAccountId(accountId, accountId, pageable)
                .map(transactionMapper::toResponse);

        log.debug("[TransactionServiceImpl#findByAccountId] EXIT - totalElements={}", page.getTotalElements());
        return page;
    }
}

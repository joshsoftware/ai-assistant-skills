package com.joshsoftware.mockbank.service.impl;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.dto.request.CreateAccountRequestDTO;
import com.joshsoftware.mockbank.dto.request.UpdateAccountStatusRequestDTO;
import com.joshsoftware.mockbank.dto.response.AccountResponseDTO;
import com.joshsoftware.mockbank.entity.Account;
import com.joshsoftware.mockbank.entity.Customer;
import com.joshsoftware.mockbank.enums.AccountStatus;
import com.joshsoftware.mockbank.exception.ResourceNotFoundException;
import com.joshsoftware.mockbank.mapper.AccountMapper;
import com.joshsoftware.mockbank.repository.AccountRepository;
import com.joshsoftware.mockbank.repository.CustomerRepository;
import com.joshsoftware.mockbank.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository  accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountMapper      accountMapper;

    // Creates a new account linked to the given customer.
    @Override
    @Transactional
    public AccountResponseDTO create(CreateAccountRequestDTO request) {
        log.debug("[AccountServiceImpl#create] ENTRY - customerId={}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        Account account = accountMapper.toEntity(request, customer);
        account.setAccountNumber(generateAccountNumber());
        Account createdAccount = accountRepository.save(account);

        log.debug("[AccountServiceImpl#create] EXIT - accountId={}", createdAccount.getId());
        return accountMapper.toResponse(createdAccount);
    }

    // Retrieves an account by UUID.
    @Override
    public AccountResponseDTO findById(UUID id) {
        log.debug("[AccountServiceImpl#findById] ENTRY - accountId={}", id);

        AccountResponseDTO response = accountRepository.findById(id)
                .map(accountMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        log.debug("[AccountServiceImpl#findById] EXIT - accountId={}", id);
        return response;
    }

    // Returns all accounts for a given customer UUID.
    @Override
    public List<AccountResponseDTO> findByCustomerId(UUID customerId) {
        log.debug("[AccountServiceImpl#findByCustomerId] ENTRY - customerId={}", customerId);

        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "id", customerId);
        }

        List<AccountResponseDTO> accounts = accountRepository.findByCustomerId(customerId)
                .stream()
                .map(accountMapper::toResponse)
                .toList();

        log.debug("[AccountServiceImpl#findByCustomerId] EXIT - count={}", accounts.size());
        return accounts;
    }

    // Updates the status of an account to ACTIVE, INACTIVE, or FROZEN.
    @Override
    @Transactional
    public AccountResponseDTO updateStatus(UUID id, UpdateAccountStatusRequestDTO request) {
        log.debug("[AccountServiceImpl#updateStatus] ENTRY - accountId={}, status={}", id, request.getStatus());

        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        existing.setStatus(AccountStatus.valueOf(request.getStatus()));
        Account updatedAccount = accountRepository.save(existing);

        log.debug("[AccountServiceImpl#updateStatus] EXIT - accountId={}", id);
        return accountMapper.toResponse(updatedAccount);
    }

    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis();
    }
}

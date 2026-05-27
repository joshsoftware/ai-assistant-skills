package com.joshsoftware.mockbank.service;

import com.joshsoftware.mockbank.dto.request.CreateAccountRequestDTO;
import com.joshsoftware.mockbank.dto.request.UpdateAccountStatusRequestDTO;
import com.joshsoftware.mockbank.dto.response.AccountResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    // Creates a new account linked to a customer.
    AccountResponseDTO create(CreateAccountRequestDTO request);

    // Retrieves an account by its UUID.
    AccountResponseDTO findById(UUID id);

    // Returns all accounts belonging to a specific customer.
    List<AccountResponseDTO> findByCustomerId(UUID customerId);

    // Updates the status of an account (ACTIVE / INACTIVE / FROZEN).
    AccountResponseDTO updateStatus(UUID id, UpdateAccountStatusRequestDTO request);
}

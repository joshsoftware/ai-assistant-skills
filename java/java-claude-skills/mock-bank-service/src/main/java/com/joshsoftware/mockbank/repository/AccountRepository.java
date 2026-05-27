package com.joshsoftware.mockbank.repository;

import com.joshsoftware.mockbank.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(UUID customerId);
}

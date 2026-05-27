package com.joshsoftware.mockbank.repository;

import com.joshsoftware.mockbank.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByFromAccountIdOrToAccountId(UUID fromAccountId, UUID toAccountId, Pageable pageable);
}

package com.joshsoftware.mockbank.service.impl;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.dto.request.InitiatePaymentRequestDTO;
import com.joshsoftware.mockbank.dto.response.PaymentResponseDTO;
import com.joshsoftware.mockbank.entity.Account;
import com.joshsoftware.mockbank.entity.Payment;
import com.joshsoftware.mockbank.entity.Transaction;
import com.joshsoftware.mockbank.enums.AccountStatus;
import com.joshsoftware.mockbank.enums.PaymentMode;
import com.joshsoftware.mockbank.enums.PaymentStatus;
import com.joshsoftware.mockbank.enums.TransactionStatus;
import com.joshsoftware.mockbank.enums.TransactionType;
import com.joshsoftware.mockbank.exception.BusinessException;
import com.joshsoftware.mockbank.exception.ResourceNotFoundException;
import com.joshsoftware.mockbank.mapper.PaymentMapper;
import com.joshsoftware.mockbank.repository.AccountRepository;
import com.joshsoftware.mockbank.repository.PaymentRepository;
import com.joshsoftware.mockbank.repository.TransactionRepository;
import com.joshsoftware.mockbank.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository     paymentRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository     accountRepository;
    private final PaymentMapper         paymentMapper;

    // Initiates a payment: debits fromAccount, credits toAccount, persists Transaction and Payment.
    @Override
    @Transactional
    public PaymentResponseDTO initiate(InitiatePaymentRequestDTO request) {
        log.debug("[PaymentServiceImpl#initiate] ENTRY - from={}, to={}, amount={}", request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getFromAccountId()));

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.getToAccountId()));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Source account is not active");
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("Destination account is not active");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("Insufficient balance in source account");
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN" + System.currentTimeMillis());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        Transaction createdTransaction = transactionRepository.save(transaction);

        Payment payment = new Payment();
        payment.setPaymentReference("PAY" + System.currentTimeMillis());
        payment.setPaymentMode(PaymentMode.valueOf(request.getPaymentMode()));
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransaction(createdTransaction);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        Payment createdPayment = paymentRepository.save(payment);

        log.debug("[PaymentServiceImpl#initiate] EXIT - paymentId={}", createdPayment.getId());
        return paymentMapper.toResponse(createdPayment);
    }

    // Retrieves a payment by UUID.
    @Override
    public PaymentResponseDTO findById(UUID id) {
        log.debug("[PaymentServiceImpl#findById] ENTRY - paymentId={}", id);

        PaymentResponseDTO response = paymentRepository.findById(id)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));

        log.debug("[PaymentServiceImpl#findById] EXIT - paymentId={}", id);
        return response;
    }

    // Returns all payments as a paginated list.
    @Override
    public Page<PaymentResponseDTO> findAll(Pageable pageable) {
        log.debug("[PaymentServiceImpl#findAll] ENTRY - page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<PaymentResponseDTO> page = paymentRepository.findAll(pageable)
                .map(paymentMapper::toResponse);

        log.debug("[PaymentServiceImpl#findAll] EXIT - totalElements={}", page.getTotalElements());
        return page;
    }
}

package com.example.tm.expense.service;

import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.expense.dto.ExpenseRequestDto;
import com.example.tm.expense.dto.ExpenseResponseDto;
import com.example.tm.expense.entity.Expense;
import com.example.tm.expense.repo.ExpenseRepository;
import com.example.tm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements service logic for expense service impl.
 */
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "tmTransactionManager")
public class ExpenseServiceImpl implements ExpenseService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_SENT_BACK = "SENT_BACK";
    private static final Set<String> SUPPORTED_STATUSES = Set.of(
            STATUS_PENDING,
            STATUS_APPROVED,
            STATUS_SENT_BACK);

    private final ExpenseRepository expenseRepository;
    private final TmUserRepository tmUserRepository;

    /** Handles create. */
    @Override
    public ExpenseResponseDto create(ExpenseRequestDto requestDto, String actorRole) {
        requireActorRole(actorRole);
        requireTechnicianRole(actorRole);

        Expense entity = new Expense();
        populateEntity(requestDto, entity);
        entity.setStatus(STATUS_PENDING);
        entity.setSubmittedAt(Instant.now());
        entity.setApprovedAt(null);

        return toResponse(expenseRepository.save(entity));
    }

    /** Handles update. */
    @Override
    public ExpenseResponseDto update(Long id, ExpenseRequestDto requestDto, String actorRole) {
        requireActorRole(actorRole);
        requireTechnicianRole(actorRole);

        Expense existing = findByIdOrThrow(id);
        ensureEditable(existing);

        populateEntity(requestDto, existing);
        return toResponse(expenseRepository.save(existing));
    }

    /** Handles submit. */
    @Override
    public ExpenseResponseDto submit(Long id, String actorRole) {
        requireActorRole(actorRole);
        requireTechnicianRole(actorRole);

        Expense existing = findByIdOrThrow(id);
        String normalizedStatus = normalizeStatus(existing.getStatus());
        if (!STATUS_PENDING.equals(normalizedStatus) && !STATUS_SENT_BACK.equals(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending or sent-back expenses can be submitted");
        }

        existing.setStatus(STATUS_PENDING);
        if (existing.getSubmittedAt() == null) {
            existing.setSubmittedAt(Instant.now());
        }
        existing.setApprovedAt(null);
        return toResponse(expenseRepository.save(existing));
    }

    /** Handles approve. */
    @Override
    public ExpenseResponseDto approve(Long id, String actorRole) {
        requireActorRole(actorRole);
        requireManagerRole(actorRole);

        Expense existing = findByIdOrThrow(id);
        if (!STATUS_PENDING.equals(normalizeStatus(existing.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending expenses can be approved");
        }

        existing.setStatus(STATUS_APPROVED);
        existing.setApprovedAt(Instant.now());
        return toResponse(expenseRepository.save(existing));
    }

    /** Sends back to technician. */
    @Override
    public ExpenseResponseDto sendBack(Long id, String actorRole) {
        requireActorRole(actorRole);
        requireManagerRole(actorRole);

        Expense existing = findByIdOrThrow(id);
        if (!STATUS_PENDING.equals(normalizeStatus(existing.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending expenses can be sent back");
        }

        existing.setStatus(STATUS_SENT_BACK);
        existing.setApprovedAt(null);
        return toResponse(expenseRepository.save(existing));
    }

    /** Returns all expenses. */
    @Override
    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public List<ExpenseResponseDto> getAll(String actorRole, String status) {
        requireActorRole(actorRole);
        requireManagerRole(actorRole);

        List<Expense> expenses = status == null || status.isBlank()
                ? expenseRepository.findAllByOrderByExpenseDateDescIdDesc()
                : expenseRepository.findByStatusOrderByExpenseDateDescIdDesc(normalizeStatus(status));

        return expenses.stream().map(this::toResponse).toList();
    }

    /** Returns expense by id. */
    @Override
    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public ExpenseResponseDto getById(Long id, String actorRole) {
        requireActorRole(actorRole);
        return toResponse(findByIdOrThrow(id));
    }

    /** Returns expenses by user id. */
    @Override
    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public List<ExpenseResponseDto> getByUserId(Long userId, String actorRole) {
        requireActorRole(actorRole);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id is required");
        }
        return expenseRepository.findByUserIdOrderByExpenseDateDescIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Handles delete. */
    @Override
    public void delete(Long id, String actorRole) {
        requireActorRole(actorRole);
        requireTechnicianRole(actorRole);

        Expense existing = findByIdOrThrow(id);
        ensureEditable(existing);
        expenseRepository.delete(existing);
    }

    /** Finds by id or throw. */
    private Expense findByIdOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));
    }

    /** Validates expense state for modifications. */
    private void ensureEditable(Expense expense) {
        String normalizedStatus = normalizeStatus(expense.getStatus());
        if (STATUS_APPROVED.equals(normalizedStatus) || STATUS_PENDING.equals(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only sent-back expenses can be modified");
        }
    }

    /** Populates entity. */
    private void populateEntity(ExpenseRequestDto requestDto, Expense entity) {
        String expenseCode = trimToNull(requestDto.getExpenseCode());
        String description = trimToNull(requestDto.getDescription());
        if (expenseCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expense_code is required");
        }
        if (description == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }
        entity.setExpenseDate(requestDto.getDate());
        entity.setExpenseCode(expenseCode);
        entity.setDescription(description);
        entity.setAmount(requestDto.getAmount());
        entity.setUserId(requestDto.getUserId());
        entity.setWorkOrderId(requestDto.getWorkOrderId());
        entity.setWorkOrderName(trimToNull(requestDto.getWorkOrderName()));
    }

    /** Converts data to response. */
    private ExpenseResponseDto toResponse(Expense entity) {
        TmUser user = entity.getUserId() == null
                ? null
                : tmUserRepository.findById(entity.getUserId()).orElse(null);
        String userFirstName = user == null ? null : user.getFirstName();
        String userLastName = user == null ? null : user.getLastName();
        String userName = (userFirstName == null && userLastName == null)
                ? null
                : String.join(" ", userFirstName == null ? "" : userFirstName,
                        userLastName == null ? "" : userLastName).trim();

        return ExpenseResponseDto.builder()
                .id(entity.getId())
                .date(entity.getExpenseDate())
                .expenseCode(entity.getExpenseCode())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .userId(entity.getUserId())
                .userFirstName(userFirstName)
                .userLastName(userLastName)
                .userName(userName)
                .workOrderId(entity.getWorkOrderId())
                .workOrderName(entity.getWorkOrderName())
                .status(normalizeStatus(entity.getStatus()))
                .submittedAt(entity.getSubmittedAt())
                .approvedAt(entity.getApprovedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /** Handles trim to null. */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Normalizes status. */
    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_PENDING;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported expense status: " + status);
        }
        return normalized;
    }

    /** Validates actor role. */
    private void requireActorRole(String actorRole) {
        if (actorRole == null || actorRole.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Role claim not present in token");
        }
    }

    /** Checks whether technician role. */
    private boolean isTechnicianRole(String actorRole) {
        String normalized = actorRole == null ? "" : actorRole.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("TECHNICIAN") || normalized.contains("TECHNICIAN");
    }

    /** Checks whether admin role. */
    private boolean isAdminRole(String actorRole) {
        String normalized = actorRole == null ? "" : actorRole.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ADMIN") || normalized.contains("ADMIN");
    }

    /** Checks whether manager role. */
    private boolean isManagerRole(String actorRole) {
        String normalized = actorRole == null ? "" : actorRole.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("MANAGER")
                || normalized.contains("MANAGER")
                || normalized.equals("ADMIN")
                || normalized.contains("ADMIN");
    }

    /** Validates technician role. */
    private void requireTechnicianRole(String actorRole) {
        if (!isTechnicianRole(actorRole) && !isAdminRole(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only technician or admin can perform this action");
        }
    }

    /** Validates manager role. */
    private void requireManagerRole(String actorRole) {
        if (!isManagerRole(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only manager can perform this action");
        }
    }
}

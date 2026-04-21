package com.example.tm.expense.service;

import com.example.tm.expense.dto.ExpenseRequestDto;
import com.example.tm.expense.dto.ExpenseResponseDto;
import java.util.List;

/**
 * Defines operations for expense service.
 */
public interface ExpenseService {

    ExpenseResponseDto create(ExpenseRequestDto requestDto, String actorRole);

    ExpenseResponseDto update(Long id, ExpenseRequestDto requestDto, String actorRole);

    ExpenseResponseDto submit(Long id, String actorRole);

    ExpenseResponseDto approve(Long id, String actorRole);

    ExpenseResponseDto sendBack(Long id, String actorRole);

    List<ExpenseResponseDto> getAll(String actorRole, String status);

    ExpenseResponseDto getById(Long id, String actorRole);

    List<ExpenseResponseDto> getByUserId(Long userId, String actorRole);

    void delete(Long id, String actorRole);
}

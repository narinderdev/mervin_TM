package com.example.tm.expense.repo;

import com.example.tm.expense.entity.Expense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Defines operations for expense repository.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByOrderByExpenseDateDescIdDesc();

    List<Expense> findByStatusOrderByExpenseDateDescIdDesc(String status);

    List<Expense> findByUserIdOrderByExpenseDateDescIdDesc(Long userId);
}

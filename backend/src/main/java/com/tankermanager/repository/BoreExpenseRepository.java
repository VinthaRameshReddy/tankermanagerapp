package com.tankermanager.repository;

import com.tankermanager.entity.BoreExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BoreExpenseRepository extends JpaRepository<BoreExpense, Long> {
    List<BoreExpense> findByOperatorIdOrderByExpenseDateDesc(Long operatorId);
    List<BoreExpense> findByBoreIdOrderByExpenseDateDesc(Long boreId);

    @Query("select coalesce(sum(b.amount), 0) from BoreExpense b where b.operator.id = :operatorId and b.expenseDate between :from and :to")
    BigDecimal sumByOperatorAndDateRange(Long operatorId, LocalDate from, LocalDate to);
}

package com.tankermanager.repository;

import com.tankermanager.entity.Expense;
import com.tankermanager.enums.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByOperatorIdOrderByExpenseDateDesc(Long operatorId);
    List<Expense> findByTankerIdOrderByExpenseDateDesc(Long tankerId);
    List<Expense> findByOperatorIdAndType(Long operatorId, ExpenseType type);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.operator.id = :operatorId and e.expenseDate between :from and :to")
    BigDecimal sumByOperatorAndDateRange(Long operatorId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.tanker.id = :tankerId")
    BigDecimal sumByTanker(Long tankerId);
}

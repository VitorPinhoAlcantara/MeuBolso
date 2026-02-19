package br.com.meubolso.service;

import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.ExpenseByAccountItem;
import br.com.meubolso.dto.ExpenseByCategoryItem;
import br.com.meubolso.dto.MonthlyReportResponse;
import br.com.meubolso.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public MonthlyReportResponse getMonthlyReport(UUID userId, int year, int month) {
        validatePeriod(year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        BigDecimal totalIncome = transactionRepository
                .sumAmountByTypeAndPeriod(
                        userId,
                        TransactionType.INCOME.name(),
                        year,
                        month,
                        startDate,
                        endDate
                );

        BigDecimal totalExpense = transactionRepository
                .sumAmountByTypeAndPeriod(
                        userId,
                        TransactionType.EXPENSE.name(),
                        year,
                        month,
                        startDate,
                        endDate
                );

        BigDecimal net = totalIncome.subtract(totalExpense);

        List<ExpenseByAccountItem> expensesByAccount = transactionRepository
                .sumExpensesByAccount(
                        userId,
                        year,
                        month,
                        startDate,
                        endDate,
                        TransactionType.EXPENSE.name()
                )
                .stream()
                .map(p -> {
                    ExpenseByAccountItem item = new ExpenseByAccountItem();
                    item.setAccountId(p.getAccountId());
                    item.setAccountName(p.getAccountName());
                    item.setTotal(p.getTotal());
                    return item;
                })
                .toList();

        List<ExpenseByCategoryItem> expensesByCategory = transactionRepository
                .sumExpensesByCategory(
                        userId,
                        year,
                        month,
                        startDate,
                        endDate,
                        TransactionType.EXPENSE.name()
                )
                .stream()
                .map(p -> {
                    ExpenseByCategoryItem item = new ExpenseByCategoryItem();
                    item.setCategoryId(p.getCategoryId());
                    item.setCategoryName(p.getCategoryName());
                    item.setCategoryColor(p.getCategoryColor());
                    item.setTotal(p.getTotal());
                    return item;
                })
                .toList();

        MonthlyReportResponse response = new MonthlyReportResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        response.setNet(net);
        response.setExpensesByAccount(expensesByAccount);
        response.setExpensesByCategory(expensesByCategory);

        return response;
    }

    private void validatePeriod(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mês inválido");
        }
        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ano inválido");
        }
    }
}

package br.com.meubolso.service;

import br.com.meubolso.domain.enums.TransactionType;
import br.com.meubolso.dto.MonthlyReportResponse;
import br.com.meubolso.repository.ExpenseByCategoryProjection;
import br.com.meubolso.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void shouldBuildMonthlyReportWithNetAndCategoryTotals() {
        UUID userId = UUID.randomUUID();

        when(transactionRepository.sumAmountByTypeAndPeriod(eq(userId), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.sumAmountByTypeAndPeriod(eq(userId), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1800.00"));
        when(transactionRepository.sumExpensesByCategory(eq(userId), any(LocalDate.class), any(LocalDate.class), eq(TransactionType.EXPENSE)))
                .thenReturn(List.of(new ExpenseByCategoryProjection() {
                    @Override
                    public UUID getCategoryId() {
                        return UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
                    }

                    @Override
                    public String getCategoryName() {
                        return "Food";
                    }

                    @Override
                    public String getCategoryColor() {
                        return "#16A34A";
                    }

                    @Override
                    public BigDecimal getTotal() {
                        return new BigDecimal("1200.00");
                    }
                }));

        MonthlyReportResponse response = reportService.getMonthlyReport(userId, 2026, 2);

        assertEquals(2026, response.getYear());
        assertEquals(2, response.getMonth());
        assertEquals(new BigDecimal("5000.00"), response.getTotalIncome());
        assertEquals(new BigDecimal("1800.00"), response.getTotalExpense());
        assertEquals(new BigDecimal("3200.00"), response.getNet());
        assertEquals(1, response.getExpensesByCategory().size());
        assertEquals("Food", response.getExpensesByCategory().get(0).getCategoryName());
    }
}

package br.com.meubolso.dto;

import java.math.BigDecimal;
import java.util.List;


public class MonthlyReportResponse {
    
    private int year;
    private int month;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal net;
    private List<ExpenseByAccountItem> expensesByAccount;
    private List<ExpenseByCategoryItem> expensesByCategory;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNet() {
        return net;
    }

    public void setNet(BigDecimal net) {
        this.net = net;
    }

    public List<ExpenseByAccountItem> getExpensesByAccount() {
        return expensesByAccount;
    }

    public void setExpensesByAccount(List<ExpenseByAccountItem> expensesByAccount) {
        this.expensesByAccount = expensesByAccount;
    }

    public List<ExpenseByCategoryItem> getExpensesByCategory() {
        return expensesByCategory;
    }

    public void setExpensesByCategory(List<ExpenseByCategoryItem> expensesByCategory) {
        this.expensesByCategory = expensesByCategory;
    }
}

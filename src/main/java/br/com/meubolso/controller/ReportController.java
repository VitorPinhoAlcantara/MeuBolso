package br.com.meubolso.controller;

import br.com.meubolso.dto.MonthlyReportResponse;
import br.com.meubolso.security.AuthenticatedUser;
import br.com.meubolso.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public MonthlyReportResponse getMonthlyReport(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return reportService.getMonthlyReport(currentUser.userId(), year, month);
    }
}

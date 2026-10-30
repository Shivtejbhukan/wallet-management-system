
package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.response.AdminWalletResponse;
import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Tag(
        name = "Admin APIs",
        description = "APIs for administrators to manage and monitor wallets and transactions"
)
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(
            summary = "Get all users' wallets",
            description = "Returns wallet details of all registered users. Admin access required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    @GetMapping("/wallets")
    public ResponseEntity<List<AdminWalletResponse>> getAllWallets() {

        List<AdminWalletResponse> wallets =
                adminService.getAllWallets();

        return ResponseEntity.ok(wallets);
    }

    @Operation(
            summary = "Get all transactions",
            description = "Returns all wallet transactions with pagination. Admin access required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(

            @Parameter(description = "Page number (starts from 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Page<TransactionResponse> transactions =
                adminService.getAllTransactions(page, size);

        return ResponseEntity.ok(transactions);
    }
}

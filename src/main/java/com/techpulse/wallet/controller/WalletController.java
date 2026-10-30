
package com.techpulse.wallet.controller;

import com.techpulse.wallet.dto.request.AddMoneyRequest;
import com.techpulse.wallet.dto.request.TransferRequest;
import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.dto.response.WalletResponse;
import com.techpulse.wallet.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.data.domain.Page;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
@Tag(
        name = "Wallet APIs",
        description = "APIs for wallet balance, adding money, transfers and transaction history"
)
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @Operation(
            summary = "Get my wallet",
            description = "Returns the authenticated user's wallet balance and version."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Wallet not found")
    })
    @GetMapping
    public ResponseEntity<WalletResponse> getMyWallet(
            Authentication authentication) {

        WalletResponse response =
                walletService.getMyWallet(authentication.getName());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Add money to wallet",
            description = "Adds money to the authenticated user's wallet. An Idempotency-Key is required to prevent duplicate transactions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Money added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid amount or idempotency key"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    })
    @PostMapping("/add")
    public ResponseEntity<TransactionResponse> addMoney(

            @Valid @RequestBody AddMoneyRequest request,

            @Parameter(
                    description = "Unique key for this add-money request. Reuse the same key only when retrying the same request.",
                    required = true,
                    example = "add-money-123"
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey,

            Authentication authentication) {

        TransactionResponse response = walletService.addMoney(
                authentication.getName(),
                request.amount(),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Transfer money",
            description = "Transfers money from the authenticated user's wallet to another user's wallet. The transfer is atomic and requires an Idempotency-Key."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Money transferred successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid transfer details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "Sender or receiver wallet not found"),
            @ApiResponse(responseCode = "409", description = "Insufficient balance or idempotency key conflict")
    })
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transferMoney(

            @Valid @RequestBody TransferRequest request,

            @Parameter(
                    description = "Unique key for this transfer request. Reuse the same key only when retrying the same request.",
                    required = true,
                    example = "transfer-money-123"
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey,

            Authentication authentication) {

        TransactionResponse response = walletService.transferMoney(
                authentication.getName(),
                request.receiverUserId(),
                request.amount(),
                idempotencyKey
        );

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get my transactions",
            description = "Returns transaction history belonging to the authenticated user with pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid")
    })
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(

            Authentication authentication,

            @Parameter(description = "Page number (starts from 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page (maximum 100)", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Page<TransactionResponse> transactions =
                walletService.getMyTransactions(
                        authentication.getName(),
                        page,
                        size
                );

        return ResponseEntity.ok(transactions);
    }
}

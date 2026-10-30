package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.response.AdminWalletResponse;
import com.techpulse.wallet.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdminService {

    List<AdminWalletResponse> getAllWallets();

    Page<TransactionResponse> getAllTransactions(int page, int size);
}
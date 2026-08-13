package com.tienvm.trade.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tienvm.trade.entity.UserWallet;

public record WalletResponse(
		UUID userId,
		BigDecimal balance,
		BigDecimal heldBalance,
		BigDecimal availableBalance,
		Instant updatedAt
) {
	public static WalletResponse from(UserWallet wallet) {
		BigDecimal available = wallet.getBalance().subtract(wallet.getHeldBalance());
		return new WalletResponse(
				wallet.getUserId(),
				wallet.getBalance(),
				wallet.getHeldBalance(),
				available.compareTo(BigDecimal.ZERO) > 0 ? available : BigDecimal.ZERO,
				wallet.getUpdatedAt()
		);
	}
}

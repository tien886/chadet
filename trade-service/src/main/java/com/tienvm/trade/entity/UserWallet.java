package com.tienvm.trade.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_wallets")
public class UserWallet {

	@Id
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO;

	@Column(name = "held_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal heldBalance = BigDecimal.ZERO;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public UserWallet(UUID userId, BigDecimal balance) {
		this.userId = userId;
		this.balance = balance != null ? balance : BigDecimal.ZERO;
		this.heldBalance = BigDecimal.ZERO;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

}

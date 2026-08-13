package com.tienvm.trade.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.tienvm.trade.dto.WalletResponse;
import com.tienvm.trade.entity.UserWallet;
import com.tienvm.trade.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

	@Mock
	private UserWalletRepository userWalletRepository;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private WalletService walletService;

	@BeforeEach
	void setUp() {
		walletService = new WalletService(userWalletRepository, messagingTemplate);
	}

	@Test
	void getOrCreateWallet_returnsExisting() {
		UUID userId = UUID.randomUUID();
		UserWallet wallet = new UserWallet(userId, new BigDecimal("500.00"));
		when(userWalletRepository.findById(userId)).thenReturn(Optional.of(wallet));

		UserWallet result = walletService.getOrCreateWallet(userId);

		assertThat(result.getBalance()).isEqualTo(new BigDecimal("500.00"));
	}

	@Test
	void getOrCreateWallet_createsNewDefault() {
		UUID userId = UUID.randomUUID();
		when(userWalletRepository.findById(userId)).thenReturn(Optional.empty());
		when(userWalletRepository.save(any(UserWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserWallet result = walletService.getOrCreateWallet(userId);

		assertThat(result.getBalance()).isEqualTo(new BigDecimal("1000.00"));
		verify(userWalletRepository).save(any(UserWallet.class));
	}

	@Test
	void deposit_increasesBalance() {
		UUID userId = UUID.randomUUID();
		UserWallet wallet = new UserWallet(userId, new BigDecimal("100.00"));
		when(userWalletRepository.findById(userId)).thenReturn(Optional.of(wallet));
		when(userWalletRepository.save(any(UserWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

		WalletResponse response = walletService.deposit(userId, new BigDecimal("50.00"));

		assertThat(response.balance()).isEqualTo(new BigDecimal("150.00"));
	}

	@Test
	void holdBalance_throwsWhenInsufficient() {
		UUID userId = UUID.randomUUID();
		UserWallet wallet = new UserWallet(userId, new BigDecimal("100.00"));
		when(userWalletRepository.findById(userId)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> walletService.holdBalance(userId, new BigDecimal("200.00")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Insufficient available balance");
	}

	@Test
	void holdBalance_holdsFunds() {
		UUID userId = UUID.randomUUID();
		UserWallet wallet = new UserWallet(userId, new BigDecimal("500.00"));
		when(userWalletRepository.findById(userId)).thenReturn(Optional.of(wallet));
		when(userWalletRepository.save(any(UserWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

		walletService.holdBalance(userId, new BigDecimal("200.00"));

		assertThat(wallet.getHeldBalance()).isEqualTo(new BigDecimal("200.00"));
	}

	@Test
	void settleTransfer_deductsFromSenderAndAddsToReceiver() {
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();

		UserWallet sender = new UserWallet(senderId, new BigDecimal("500.00"));
		sender.setHeldBalance(new BigDecimal("100.00"));

		UserWallet receiver = new UserWallet(receiverId, new BigDecimal("200.00"));

		when(userWalletRepository.findById(senderId)).thenReturn(Optional.of(sender));
		when(userWalletRepository.findById(receiverId)).thenReturn(Optional.of(receiver));

		walletService.settleTransfer(senderId, receiverId, new BigDecimal("100.00"));

		assertThat(sender.getBalance()).isEqualTo(new BigDecimal("400.00"));
		assertThat(sender.getHeldBalance()).isEqualTo(BigDecimal.ZERO);
		assertThat(receiver.getBalance()).isEqualTo(new BigDecimal("300.00"));
	}

}

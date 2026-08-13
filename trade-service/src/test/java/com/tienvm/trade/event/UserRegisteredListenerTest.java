package com.tienvm.trade.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tienvm.trade.entity.UserWallet;
import com.tienvm.trade.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisteredListenerTest {

	@Mock
	private UserWalletRepository userWalletRepository;

	private UserRegisteredListener listener;

	@BeforeEach
	void setUp() {
		listener = new UserRegisteredListener(userWalletRepository);
	}

	@Test
	void handleUserRegistered_createsWalletWhenNotExists() {
		UUID userId = UUID.randomUUID();
		UserRegisteredEvent event = new UserRegisteredEvent(
				UUID.randomUUID(), "USER_REGISTERED", userId, "test@gmail.com", "testuser", Instant.now()
		);

		when(userWalletRepository.existsById(userId)).thenReturn(false);

		listener.handleUserRegistered(event);

		ArgumentCaptor<UserWallet> captor = ArgumentCaptor.forClass(UserWallet.class);
		verify(userWalletRepository).save(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo(userId);
		assertThat(captor.getValue().getBalance()).isEqualTo(new BigDecimal("1000.00"));
	}

}

package com.tienvm.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.tienvm.auth.config.RabbitMQConfig;
import com.tienvm.auth.entity.User;
import com.tienvm.auth.entity.UserRepository;
import com.tienvm.auth.event.UserRegisteredEvent;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {

	private final UserRepository users = mock(UserRepository.class);
	private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
	private final AuthService auth = new AuthService(users, new BCryptPasswordEncoder(), mock(TokenService.class), rabbitTemplate);

	@Test
	void registerHashesPassword() {
		when(users.existsByGmail("a@b.com")).thenReturn(false);
		when(users.existsByUsername("alice")).thenReturn(false);
		when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		User saved = auth.register("a@b.com", "alice", "secret123");

		assertThat(saved.getPassword()).isNotEqualTo("secret123");
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq(RabbitMQConfig.ROUTING_KEY_USER_REGISTERED), any(UserRegisteredEvent.class));
		when(users.findByGmail("a@b.com")).thenReturn(Optional.of(saved));

		assertThat(auth.login("a@b.com", "secret123").getId()).isEqualTo(saved.getId());
		assertThatThrownBy(() -> auth.login("a@b.com", "wrong")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void registerRejectsDuplicateGmail() {
		when(users.existsByGmail("a@b.com")).thenReturn(true);

		assertThatThrownBy(() -> auth.register("a@b.com", "alice", "secret123"))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
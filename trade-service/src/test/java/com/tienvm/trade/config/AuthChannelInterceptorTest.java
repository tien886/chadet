package com.tienvm.trade.config;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthChannelInterceptorTest {

	@Mock
	private JwtDecoder jwtDecoder;

	@Mock
	private MessageChannel channel;

	private AuthChannelInterceptor interceptor;

	@BeforeEach
	void setUp() {
		interceptor = new AuthChannelInterceptor(jwtDecoder);
	}

	@Test
	void preSend_validBearerToken_setsAuthenticatedUser() {
		String userId = UUID.randomUUID().toString();
		String token = "valid-test-token";

		Jwt jwt = new Jwt(
				token,
				Instant.now(),
				Instant.now().plusSeconds(3600),
				Map.of("alg", "RS256"),
				Map.of("sub", userId, "username", "alice")
		);

		when(jwtDecoder.decode(eq(token))).thenReturn(jwt);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", "Bearer " + token);
		accessor.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, channel);

		assertThat(result).isNotNull();
		StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
		assertThat(resultAccessor.getUser()).isNotNull();
		assertThat(resultAccessor.getUser().getName()).isEqualTo(userId);
	}

	@Test
	void preSend_invalidToken_userRemainsNull() {
		String token = "invalid-token";
		when(jwtDecoder.decode(eq(token))).thenThrow(new JwtException("Invalid signature"));

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", "Bearer " + token);
		accessor.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, channel);

		assertThat(result).isNotNull();
		StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
		assertThat(resultAccessor.getUser()).isNull();
	}

	@Test
	void preSend_nonConnectCommand_ignoresAuth() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, channel);

		assertThat(result).isNotNull();
	}

	@Test
	void preSend_connectWithoutAuthHeader_passesThrough() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		Message<?> result = interceptor.preSend(message, channel);

		assertThat(result).isNotNull();
		StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
		assertThat(resultAccessor.getUser()).isNull();
	}

}

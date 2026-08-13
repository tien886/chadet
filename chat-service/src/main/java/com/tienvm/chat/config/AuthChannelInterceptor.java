package com.tienvm.chat.config;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

	private final JwtDecoder jwtDecoder;

	public AuthChannelInterceptor(JwtDecoder jwtDecoder) {
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
			log.info("STOMP CONNECT frame received");
			List<String> authHeaders = accessor.getNativeHeader("Authorization");
			if (authHeaders != null && !authHeaders.isEmpty()) {
				String authHeader = authHeaders.get(0);
				if (authHeader.startsWith("Bearer ") || authHeader.startsWith("bearer ")) {
					String token = authHeader.substring(7);
					try {
						Jwt jwt = jwtDecoder.decode(token);
						JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
						accessor.setUser(authentication);
						log.info("STOMP authenticated successfully for user: {}", jwt.getSubject());
					} catch (Exception ex) {
						log.warn("STOMP authentication failed: invalid JWT token ({})", ex.getMessage());
					}
				} else {
					log.warn("STOMP connect header Authorization does not start with Bearer");
				}
			} else {
				log.debug("STOMP connect frame without Authorization header");
			}
		}

		return message;
	}

}

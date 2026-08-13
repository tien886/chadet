package com.tienvm.auth.service;

import com.tienvm.auth.config.RabbitMQConfig;
import com.tienvm.auth.entity.User;
import com.tienvm.auth.entity.UserRepository;
import com.tienvm.auth.event.UserRegisteredEvent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final RabbitTemplate rabbitTemplate;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService,
			RabbitTemplate rabbitTemplate) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.rabbitTemplate = rabbitTemplate;
	}

	@Transactional
	public User register(String gmail, String username, String rawPassword) {
		log.debug("Checking availability for gmail: {}, username: {}", gmail, username);
		if (users.existsByGmail(gmail)) {
			log.warn("Registration rejected: gmail already registered ({})", gmail);
			throw new IllegalArgumentException("gmail already registered");
		}
		if (users.existsByUsername(username)) {
			log.warn("Registration rejected: username already taken ({})", username);
			throw new IllegalArgumentException("username already taken");
		}
		User savedUser = users.save(new User(gmail, username, passwordEncoder.encode(rawPassword)));
		log.info("Registered new user in DB -> id: {}, username: {}", savedUser.getId(), savedUser.getUsername());

		UserRegisteredEvent event = UserRegisteredEvent.of(savedUser.getId(), savedUser.getGmail(),
				savedUser.getUsername());
		try {
			rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_USER_REGISTERED,
					event);
			log.info("Published UserRegisteredEvent to exchange '{}' with routing key '{}' for userId: {}",
					RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_USER_REGISTERED, savedUser.getId());
		} catch (Exception e) {
			log.error("Failed to publish UserRegisteredEvent for userId: {}: {}", savedUser.getId(), e.getMessage());
		}

		return savedUser;
	}

	public User login(String gmail, String rawPassword) {
		log.debug("Authenticating user with gmail: {}", gmail);
		User user = users.findByGmail(gmail)
				.orElseThrow(() -> {
					log.warn("Login failed: gmail not found ({})", gmail);
					return new IllegalArgumentException("invalid credentials");
				});
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			log.warn("Login failed: invalid password for gmail ({})", gmail);
			throw new IllegalArgumentException("invalid credentials");
		}
		log.info("User authenticated -> id: {}, username: {}", user.getId(), user.getUsername());
		return user;
	}

	@Transactional(readOnly = true)
	public java.util.List<User> searchUsers(String query) {
		if (query == null || query.trim().isEmpty()) {
			return java.util.List.of();
		}
		log.info("Searching users with query: '{}'", query);
		return users.searchUsers(query.trim());
	}

}
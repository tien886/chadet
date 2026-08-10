package com.tienvm.auth.service;

import com.tienvm.auth.entity.User;
import com.tienvm.auth.entity.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Transactional
	public User register(String gmail, String username, String rawPassword) {
		if (users.existsByGmail(gmail)) {
			throw new IllegalArgumentException("gmail already registered");
		}
		if (users.existsByUsername(username)) {
			throw new IllegalArgumentException("username already taken");
		}
		return users.save(new User(gmail, username, passwordEncoder.encode(rawPassword)));
	}
	
	public User login(String gmail, String rawPassword) {
		User user = users.findByGmail(gmail)
				.orElseThrow(() -> new IllegalArgumentException("invalid credentials"));
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new IllegalArgumentException("invalid credentials");
		}
		return user;
	}

}
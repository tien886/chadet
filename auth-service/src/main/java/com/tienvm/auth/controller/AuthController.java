package com.tienvm.auth.controller;

import com.tienvm.auth.entity.User;
import com.tienvm.auth.service.AuthService;
import com.tienvm.auth.service.TokenService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final TokenService tokenService;

	public AuthController(AuthService authService, TokenService tokenService) {
		this.authService = authService;
		this.tokenService = tokenService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = authService.register(request.gmail(), request.username(), request.password());
		return ResponseEntity.ok(new AuthResponse(tokenService.issue(user), user.getId().toString(), user.getGmail(), user.getUsername()));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		User user = authService.login(request.gmail(), request.password());
		return ResponseEntity.ok(new AuthResponse(tokenService.issue(user), user.getId().toString(), user.getGmail(), user.getUsername()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
		return ResponseEntity.badRequest().body(ex.getFieldErrors().get(0).getDefaultMessage());
	}

	public record AuthResponse(String token, String id, String gmail, String username) {
	}

	public record RegisterRequest(
			@NotBlank @Email(message = "invalid gmail") String gmail,
			@NotBlank @Size(min = 3, max = 50, message = "username must be 3-50 chars") String username,
			@NotBlank @Size(min = 6, max = 100, message = "password must be 6-100 chars") String password) {
	}

	public record LoginRequest(
			@NotBlank @Email(message = "invalid gmail") String gmail,
			@NotBlank String password) {
	}

}
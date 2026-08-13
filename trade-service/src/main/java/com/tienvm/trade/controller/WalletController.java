package com.tienvm.trade.controller;

import java.util.UUID;

import com.tienvm.trade.dto.DepositRequest;
import com.tienvm.trade.dto.WalletResponse;
import com.tienvm.trade.service.WalletService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

	private final WalletService walletService;

	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}

	@GetMapping
	public ResponseEntity<WalletResponse> getWalletDetails(Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("GET /api/wallet -> user: {}", currentUserId);
		WalletResponse response = walletService.getWalletDetails(currentUserId);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/deposit")
	public ResponseEntity<WalletResponse> deposit(
			@Valid @RequestBody DepositRequest request,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/wallet/deposit -> user: {}, amount: {}", currentUserId, request.amount());
		WalletResponse response = walletService.deposit(currentUserId, request.amount());
		return ResponseEntity.ok(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Wallet business exception: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
		String errorMessage = ex.getFieldErrors().get(0).getDefaultMessage();
		log.warn("Wallet validation error: {}", errorMessage);
		return ResponseEntity.badRequest().body(errorMessage);
	}

	private UUID extractUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return UUID.fromString(jwt.getSubject());
		}
		throw new IllegalArgumentException("Authentication required");
	}

}

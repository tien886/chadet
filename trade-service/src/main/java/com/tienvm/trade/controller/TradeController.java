package com.tienvm.trade.controller;

import java.util.List;
import java.util.UUID;

import com.tienvm.trade.dto.CreateTradeRequest;
import com.tienvm.trade.dto.TradeResponse;
import com.tienvm.trade.service.TradeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/trades")
public class TradeController {

	private final TradeService tradeService;

	public TradeController(TradeService tradeService) {
		this.tradeService = tradeService;
	}

	@PostMapping
	public ResponseEntity<TradeResponse> createTrade(
			@Valid @RequestBody CreateTradeRequest request,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/trades -> requester: {}, receiver: {}, amount: {}", currentUserId, request.receiverId(), request.amount());
		TradeResponse response = tradeService.createTrade(request.conversationId(), currentUserId, request.receiverId(), request.amount());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/{tradeId}/confirm")
	public ResponseEntity<TradeResponse> confirmTrade(
			@PathVariable("tradeId") UUID tradeId,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/trades/{}/confirm -> user: {}", tradeId, currentUserId);
		TradeResponse response = tradeService.confirmTrade(tradeId, currentUserId);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{tradeId}/cancel")
	public ResponseEntity<TradeResponse> cancelTrade(
			@PathVariable("tradeId") UUID tradeId,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/trades/{}/cancel -> user: {}", tradeId, currentUserId);
		TradeResponse response = tradeService.cancelTrade(tradeId, currentUserId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{tradeId}")
	public ResponseEntity<TradeResponse> getTrade(
			@PathVariable("tradeId") UUID tradeId,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("GET /api/trades/{} -> user: {}", tradeId, currentUserId);
		TradeResponse response = tradeService.getTrade(tradeId, currentUserId);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<List<TradeResponse>> getUserTrades(Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("GET /api/trades -> user: {}", currentUserId);
		List<TradeResponse> response = tradeService.getUserTrades(currentUserId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/conversation/{conversationId}")
	public ResponseEntity<List<TradeResponse>> getConversationTrades(
			@PathVariable("conversationId") UUID conversationId) {
		log.info("GET /api/trades/conversation/{}", conversationId);
		List<TradeResponse> response = tradeService.getConversationTrades(conversationId);
		return ResponseEntity.ok(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Trade business exception: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
		String errorMessage = ex.getFieldErrors().get(0).getDefaultMessage();
		log.warn("Trade validation error: {}", errorMessage);
		return ResponseEntity.badRequest().body(errorMessage);
	}

	private UUID extractUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return UUID.fromString(jwt.getSubject());
		}
		throw new IllegalArgumentException("Authentication required");
	}

}

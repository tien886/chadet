package com.tienvm.chat.controller;

import java.util.UUID;

import com.tienvm.chat.dto.MessageResponse;
import com.tienvm.chat.dto.PageResponse;
import com.tienvm.chat.dto.SendMessageRequest;
import com.tienvm.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class MessageController {

	private final MessageService messageService;

	public MessageController(MessageService messageService) {
		this.messageService = messageService;
	}

	@PostMapping
	public ResponseEntity<MessageResponse> sendMessage(
			@PathVariable("conversationId") UUID conversationId,
			@Valid @RequestBody SendMessageRequest request,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/conversations/{}/messages -> sender: {}", conversationId, currentUserId);
		MessageResponse response = messageService.sendMessage(conversationId, currentUserId, request.content());
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<PageResponse<MessageResponse>> getMessageHistory(
			@PathVariable("conversationId") UUID conversationId,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "50") int size,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("GET /api/conversations/{}/messages -> requester: {}, page: {}, size: {}", conversationId, currentUserId, page, size);
		Page<MessageResponse> response = messageService.getMessageHistory(conversationId, currentUserId, page, size);
		return ResponseEntity.ok(PageResponse.fromPage(response));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Message validation or business error: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
		String errorMessage = ex.getFieldErrors().get(0).getDefaultMessage();
		log.warn("Message request validation error: {}", errorMessage);
		return ResponseEntity.badRequest().body(errorMessage);
	}

	private UUID extractUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return UUID.fromString(jwt.getSubject());
		}
		throw new IllegalArgumentException("Authentication required");
	}

}

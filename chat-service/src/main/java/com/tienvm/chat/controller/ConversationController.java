package com.tienvm.chat.controller;

import java.util.List;
import java.util.UUID;

import com.tienvm.chat.dto.AddMembersRequest;
import com.tienvm.chat.dto.ConversationResponse;
import com.tienvm.chat.dto.CreateDirectConversationRequest;
import com.tienvm.chat.dto.CreateGroupConversationRequest;
import com.tienvm.chat.service.ConversationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

	private final ConversationService conversationService;

	public ConversationController(ConversationService conversationService) {
		this.conversationService = conversationService;
	}

	@PostMapping("/direct")
	public ResponseEntity<ConversationResponse> createDirectConversation(
			@Valid @RequestBody CreateDirectConversationRequest request,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/conversations/direct -> requester: {}, recipient: {}", currentUserId, request.recipientId());
		ConversationResponse response = conversationService.getOrCreateDirectConversation(currentUserId, request.recipientId());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/group")
	public ResponseEntity<ConversationResponse> createGroupConversation(
			@Valid @RequestBody CreateGroupConversationRequest request,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/conversations/group -> creator: {}, name: '{}', members: {}", currentUserId, request.name(), request.memberIds());
		ConversationResponse response = conversationService.createGroupChat(currentUserId, request.name(), request.memberIds());
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<List<ConversationResponse>> getUserConversations(Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("GET /api/conversations -> user: {}", currentUserId);
		List<ConversationResponse> response = conversationService.getUserConversations(currentUserId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ConversationResponse> getConversationDetails(
			@PathVariable("id") UUID id,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("GET /api/conversations/{} -> user: {}", id, currentUserId);
		ConversationResponse response = conversationService.getConversationDetails(id, currentUserId);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{id}/members")
	public ResponseEntity<ConversationResponse> addMembers(
			@PathVariable("id") UUID id,
			@Valid @RequestBody AddMembersRequest request,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/conversations/{}/members -> requester: {}, newMembers: {}", id, currentUserId, request.memberIds());
		ConversationResponse response = conversationService.addMembers(id, currentUserId, request.memberIds());
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}/members/{userId}")
	public ResponseEntity<Void> removeMember(
			@PathVariable("id") UUID id,
			@PathVariable("userId") UUID userId,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("DELETE /api/conversations/{}/members/{} -> requester: {}", id, userId, currentUserId);
		conversationService.leaveConversation(id, userId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/leave")
	public ResponseEntity<Void> leaveConversation(
			@PathVariable("id") UUID id,
			Authentication authentication) {
		UUID currentUserId = extractUserId(authentication);
		log.info("POST /api/conversations/{}/leave -> user: {}", id, currentUserId);
		conversationService.leaveConversation(id, currentUserId);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Conversation business validation error: {}", ex.getMessage());
		return ResponseEntity.badRequest().body(ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<String> handleValidation(MethodArgumentNotValidException ex) {
		String errorMessage = ex.getFieldErrors().get(0).getDefaultMessage();
		log.warn("Conversation request validation error: {}", errorMessage);
		return ResponseEntity.badRequest().body(errorMessage);
	}

	private UUID extractUserId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
			return UUID.fromString(jwt.getSubject());
		}
		throw new IllegalArgumentException("Authentication required");
	}

}

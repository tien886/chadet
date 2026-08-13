package com.tienvm.chat.controller;

import java.security.Principal;
import java.util.UUID;

import com.tienvm.chat.dto.MessageResponse;
import com.tienvm.chat.dto.SendMessageRequest;
import com.tienvm.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class ChatWebSocketController {

	private final MessageService messageService;

	public ChatWebSocketController(MessageService messageService) {
		this.messageService = messageService;
	}

	@MessageMapping("/conversations/{conversationId}/messages")
	public void handleMessage(
			@DestinationVariable("conversationId") UUID conversationId,
			@Valid @Payload SendMessageRequest request,
			Principal principal) {
		UUID senderId = extractUserId(principal);
		log.info("STOMP Message received for conversation {} from sender {}", conversationId, senderId);
		messageService.sendMessage(conversationId, senderId, request.content());
	}

	private UUID extractUserId(Principal principal) {
		if (principal instanceof JwtAuthenticationToken jwtToken) {
			Jwt jwt = jwtToken.getToken();
			return UUID.fromString(jwt.getSubject());
		}
		if (principal != null && principal.getName() != null) {
			return UUID.fromString(principal.getName());
		}
		throw new IllegalArgumentException("Unauthenticated WebSocket client");
	}

}

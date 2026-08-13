package com.tienvm.chat.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record CreateDirectConversationRequest(
		@NotNull(message = "recipientId is required") UUID recipientId
) {
}

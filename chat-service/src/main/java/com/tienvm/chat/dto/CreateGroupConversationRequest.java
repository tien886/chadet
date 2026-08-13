package com.tienvm.chat.dto;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateGroupConversationRequest(
		@NotBlank(message = "Group name is required") @Size(min = 1, max = 255, message = "Group name must be between 1 and 255 characters") String name,
		@NotEmpty(message = "memberIds cannot be empty") List<UUID> memberIds
) {
}

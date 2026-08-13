package com.tienvm.chat.dto;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;

public record AddMembersRequest(
		@NotEmpty(message = "memberIds cannot be empty") List<UUID> memberIds
) {
}

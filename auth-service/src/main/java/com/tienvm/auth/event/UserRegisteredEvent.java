package com.tienvm.auth.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
		UUID eventId,
		String eventType,
		UUID userId,
		String gmail,
		String username,
		Instant timestamp
) {
	public static UserRegisteredEvent of(UUID userId, String gmail, String username) {
		return new UserRegisteredEvent(
				UUID.randomUUID(),
				"USER_REGISTERED",
				userId,
				gmail,
				username,
				Instant.now()
		);
	}
}

package com.tienvm.trade.event;

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
}

package com.tienvm.trade.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

	@Mock
	private AuthChannelInterceptor authChannelInterceptor;

	@InjectMocks
	private WebSocketConfig webSocketConfig;

	@Test
	void registerStompEndpoints_registersTradeWsEndpoints() {
		StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
		StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);

		when(registry.addEndpoint("/trade-ws")).thenReturn(registration);
		when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

		webSocketConfig.registerStompEndpoints(registry);

		verify(registry, org.mockito.Mockito.times(2)).addEndpoint("/trade-ws");
		verify(registration).withSockJS();
	}

	@Test
	void configureMessageBroker_setsPrefixes() {
		MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

		webSocketConfig.configureMessageBroker(registry);

		verify(registry).enableSimpleBroker("/topic", "/queue");
		verify(registry).setApplicationDestinationPrefixes("/app");
		verify(registry).setUserDestinationPrefix("/user");
	}

	@Test
	void configureClientInboundChannel_addsAuthInterceptor() {
		ChannelRegistration registration = mock(ChannelRegistration.class);

		webSocketConfig.configureClientInboundChannel(registration);

		verify(registration).interceptors(authChannelInterceptor);
	}

}

package com.tienvm.gateway;

import com.tienvm.gateway.filter.GatewayLoggingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
class GatewayServiceApplicationTests {

	@Autowired
	WebTestClient webTestClient;

	@Autowired
	ApplicationContext context;

	@Autowired
	GatewayLoggingFilter loggingFilter;

	@Test
	void contextLoads() {
		assertThat(context).isNotNull();
		assertThat(loggingFilter).isNotNull();
	}

	@Test
	void testUnauthenticatedRequestToSecuredRouteReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/trades/my-trades")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void testUnauthenticatedRequestToSecuredChatRouteReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/conversations")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void testUnauthenticatedRequestToSecuredWalletRouteReturnsUnauthorized() {
		webTestClient.get()
				.uri("/api/wallet")
				.exchange()
				.expectStatus().isUnauthorized();
	}

}
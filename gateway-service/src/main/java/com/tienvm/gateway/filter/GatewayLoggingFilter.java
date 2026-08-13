package com.tienvm.gateway.filter;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayLoggingFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(GatewayLoggingFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		long startTime = System.currentTimeMillis();
		var request = exchange.getRequest();

		Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
		URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
		String routeId = route != null ? route.getId() : "unknown";
		String destination = targetUri != null ? targetUri.toString()
				: (route != null ? route.getUri().toString() : "N/A");

		log.info("[Gateway] Incoming Request: {} {} -> Route: '{}' (Target: {})",
				request.getMethod(), request.getURI().getPath(), routeId, destination);

		return chain.filter(exchange)
				.doOnError(throwable -> {
					long duration = System.currentTimeMillis() - startTime;
					log.error("[Gateway] Failed Request: {} {} -> Route: '{}' | Error: {} | Duration: {}ms",
							request.getMethod(), request.getURI().getPath(), routeId,
							throwable.getMessage(), duration);
				})
				.then(Mono.fromRunnable(() -> {
					long duration = System.currentTimeMillis() - startTime;
					var statusCode = exchange.getResponse().getStatusCode();
					log.info("[Gateway] Completed Request: {} {} -> Route: '{}' | Status: {} | Duration: {}ms",
							request.getMethod(), request.getURI().getPath(), routeId,
							statusCode != null ? statusCode.value() : "N/A", duration);
				}));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}

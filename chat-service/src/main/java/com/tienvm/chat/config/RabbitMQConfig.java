package com.tienvm.chat.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	public static final String EXCHANGE_NAME = "chadet.exchange";
	public static final String QUEUE_CHAT_TRADE_EVENTS = "chat.trade-events.queue";
	public static final String ROUTING_KEY_TRADE_PATTERN = "trade.*";

	@Bean
	public TopicExchange chadetExchange() {
		return new TopicExchange(EXCHANGE_NAME, true, false);
	}

	@Bean
	public Queue chatTradeEventsQueue() {
		return new Queue(QUEUE_CHAT_TRADE_EVENTS, true);
	}

	@Bean
	public Binding bindingChatTradeEvents(Queue chatTradeEventsQueue, TopicExchange chadetExchange) {
		return BindingBuilder.bind(chatTradeEventsQueue).to(chadetExchange).with(ROUTING_KEY_TRADE_PATTERN);
	}

	@Bean
	public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
		com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
		return mapper;
	}

	@Bean
	public MessageConverter jsonMessageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
		return new Jackson2JsonMessageConverter(objectMapper);
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
			com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(jsonMessageConverter(objectMapper));
		return rabbitTemplate;
	}

}

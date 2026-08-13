package com.tienvm.trade.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import com.tienvm.trade.config.RabbitMQConfig;
import com.tienvm.trade.dto.TradeResponse;
import com.tienvm.trade.entity.Trade;
import com.tienvm.trade.entity.TradeStatus;
import com.tienvm.trade.event.TradeCompletedEvent;
import com.tienvm.trade.event.TradeCreatedEvent;
import com.tienvm.trade.event.TradeStatusChangedEvent;
import com.tienvm.trade.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

	@Mock
	private TradeRepository tradeRepository;

	@Mock
	private WalletService walletService;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	private TradeService tradeService;

	@BeforeEach
	void setUp() {
		tradeService = new TradeService(tradeRepository, walletService, rabbitTemplate, messagingTemplate);
	}

	@Test
	void createTrade_throwsWhenSelfTrade() {
		UUID userId = UUID.randomUUID();
		assertThatThrownBy(() -> tradeService.createTrade(UUID.randomUUID(), userId, userId, new BigDecimal("100.00")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("yourself");
	}

	@Test
	void createTrade_holdsBalanceAndPublishesEvent() {
		UUID convId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID tradeId = UUID.randomUUID();

		when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> {
			Trade t = invocation.getArgument(0);
			t.setId(tradeId);
			return t;
		});

		TradeResponse response = tradeService.createTrade(convId, senderId, receiverId, new BigDecimal("150.00"));

		assertThat(response.id()).isEqualTo(tradeId);
		assertThat(response.amount()).isEqualTo(new BigDecimal("150.00"));
		assertThat(response.status()).isEqualTo(TradeStatus.CREATED);

		verify(walletService).holdBalance(senderId, new BigDecimal("150.00"));
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq(RabbitMQConfig.ROUTING_KEY_TRADE_CREATED), any(TradeCreatedEvent.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/trades/" + tradeId), any(Object.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/trades/conversation/" + convId), any(Object.class));
	}

	@Test
	void confirmTrade_singlePartyConfirmation_updatesStatusAndPublishesChangedEvent() {
		UUID tradeId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();

		Trade trade = new Trade(UUID.randomUUID(), senderId, senderId, receiverId, new BigDecimal("100.00"));
		trade.setId(tradeId);

		when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));
		when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TradeResponse response = tradeService.confirmTrade(tradeId, senderId);

		assertThat(response.senderConfirmed()).isTrue();
		assertThat(response.status()).isEqualTo(TradeStatus.CONFIRMED_BY_SENDER);
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq(RabbitMQConfig.ROUTING_KEY_TRADE_STATUS_CHANGED), any(TradeStatusChangedEvent.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/trades/" + tradeId), any(Object.class));
	}

	@Test
	void confirmTrade_bothPartiesConfirmed_completesTradeAndSettlesFunds() {
		UUID tradeId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();

		Trade trade = new Trade(UUID.randomUUID(), senderId, senderId, receiverId, new BigDecimal("100.00"));
		trade.setId(tradeId);
		trade.setSenderConfirmed(true);

		when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));
		when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TradeResponse response = tradeService.confirmTrade(tradeId, receiverId);

		assertThat(response.senderConfirmed()).isTrue();
		assertThat(response.receiverConfirmed()).isTrue();
		assertThat(response.status()).isEqualTo(TradeStatus.COMPLETED);

		verify(walletService).settleTransfer(senderId, receiverId, new BigDecimal("100.00"));
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq(RabbitMQConfig.ROUTING_KEY_TRADE_COMPLETED), any(TradeCompletedEvent.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/trades/" + tradeId), any(Object.class));
	}

	@Test
	void cancelTrade_releasesHeldBalanceAndPublishesEvent() {
		UUID tradeId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();

		Trade trade = new Trade(UUID.randomUUID(), senderId, senderId, receiverId, new BigDecimal("100.00"));
		trade.setId(tradeId);

		when(tradeRepository.findById(tradeId)).thenReturn(Optional.of(trade));
		when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TradeResponse response = tradeService.cancelTrade(tradeId, senderId);

		assertThat(response.status()).isEqualTo(TradeStatus.CANCELLED);
		verify(walletService).releaseHeldBalance(senderId, new BigDecimal("100.00"));
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE_NAME), eq(RabbitMQConfig.ROUTING_KEY_TRADE_STATUS_CHANGED), any(TradeStatusChangedEvent.class));
		verify(messagingTemplate).convertAndSend(eq("/topic/trades/" + tradeId), any(Object.class));
	}

}

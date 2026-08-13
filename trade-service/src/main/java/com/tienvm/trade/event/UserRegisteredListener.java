package com.tienvm.trade.event;

import java.math.BigDecimal;

import com.tienvm.trade.config.RabbitMQConfig;
import com.tienvm.trade.entity.UserWallet;
import com.tienvm.trade.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserRegisteredListener {

	private final UserWalletRepository userWalletRepository;

	public UserRegisteredListener(UserWalletRepository userWalletRepository) {
		this.userWalletRepository = userWalletRepository;
	}

	@RabbitListener(queues = RabbitMQConfig.QUEUE_USER_REGISTRATION)
	public void handleUserRegistered(UserRegisteredEvent event) {
		log.info("Received UserRegisteredEvent from RabbitMQ -> userId: {}, username: {}", event.userId(),
				event.username());
		if (!userWalletRepository.existsById(event.userId())) {
			UserWallet wallet = new UserWallet(event.userId(), new BigDecimal("1000.00"));
			userWalletRepository.save(wallet);
			log.info("Initialized wallet with demo balance 1000.00 for user: {}", event.userId());
		} else {
			log.info("Wallet already exists for user: {}", event.userId());
		}
	}

}

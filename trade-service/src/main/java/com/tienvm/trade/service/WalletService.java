package com.tienvm.trade.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tienvm.trade.dto.WalletResponse;
import com.tienvm.trade.entity.UserWallet;
import com.tienvm.trade.repository.UserWalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WalletService {

	private final UserWalletRepository userWalletRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public WalletService(UserWalletRepository userWalletRepository, SimpMessagingTemplate messagingTemplate) {
		this.userWalletRepository = userWalletRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@Transactional
	public UserWallet getOrCreateWallet(UUID userId) {
		return userWalletRepository.findById(userId).orElseGet(() -> {
			log.info("Creating default wallet for user: {}", userId);
			UserWallet wallet = new UserWallet(userId, new BigDecimal("1000.00"));
			return userWalletRepository.save(wallet);
		});
	}

	@Transactional(readOnly = true)
	public WalletResponse getWalletDetails(UUID userId) {
		UserWallet wallet = getOrCreateWallet(userId);
		return WalletResponse.from(wallet);
	}

	@Transactional
	public WalletResponse deposit(UUID userId, BigDecimal amount) {
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Deposit amount must be positive");
		}
		UserWallet wallet = getOrCreateWallet(userId);
		wallet.setBalance(wallet.getBalance().add(amount));
		wallet.setUpdatedAt(Instant.now());
		userWalletRepository.save(wallet);
		log.info("User {} deposited {}. New balance: {}", userId, amount, wallet.getBalance());
		broadcastWalletRealtime(wallet);
		return WalletResponse.from(wallet);
	}

	@Transactional
	public void holdBalance(UUID userId, BigDecimal amount) {
		UserWallet wallet = getOrCreateWallet(userId);
		BigDecimal available = wallet.getBalance().subtract(wallet.getHeldBalance());
		if (available.compareTo(amount) < 0) {
			log.warn("Insufficient funds for user {}: available={}, requested={}", userId, available, amount);
			throw new IllegalArgumentException("Insufficient available balance for trade");
		}
		wallet.setHeldBalance(wallet.getHeldBalance().add(amount));
		wallet.setUpdatedAt(Instant.now());
		userWalletRepository.save(wallet);
		log.info("Held {} from user {} wallet. Total held: {}", amount, userId, wallet.getHeldBalance());
		broadcastWalletRealtime(wallet);
	}

	@Transactional
	public void releaseHeldBalance(UUID userId, BigDecimal amount) {
		UserWallet wallet = getOrCreateWallet(userId);
		BigDecimal newHeld = wallet.getHeldBalance().subtract(amount);
		wallet.setHeldBalance(newHeld.compareTo(BigDecimal.ZERO) > 0 ? newHeld : BigDecimal.ZERO);
		wallet.setUpdatedAt(Instant.now());
		userWalletRepository.save(wallet);
		log.info("Released held {} for user {} wallet. Remaining held: {}", amount, userId, wallet.getHeldBalance());
		broadcastWalletRealtime(wallet);
	}

	@Transactional
	public void settleTransfer(UUID senderId, UUID receiverId, BigDecimal amount) {
		UserWallet senderWallet = getOrCreateWallet(senderId);
		UserWallet receiverWallet = getOrCreateWallet(receiverId);

		// Deduct from sender balance & held balance
		senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
		BigDecimal newHeld = senderWallet.getHeldBalance().subtract(amount);
		senderWallet.setHeldBalance(newHeld.compareTo(BigDecimal.ZERO) > 0 ? newHeld : BigDecimal.ZERO);
		senderWallet.setUpdatedAt(Instant.now());

		// Add to receiver balance
		receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
		receiverWallet.setUpdatedAt(Instant.now());

		userWalletRepository.save(senderWallet);
		userWalletRepository.save(receiverWallet);
		log.info("Settled trade transfer: {} from {} to {}", amount, senderId, receiverId);

		broadcastWalletRealtime(senderWallet);
		broadcastWalletRealtime(receiverWallet);
	}

	private void broadcastWalletRealtime(UserWallet wallet) {
		try {
			WalletResponse walletResponse = WalletResponse.from(wallet);
			messagingTemplate.convertAndSendToUser(wallet.getUserId().toString(), "/queue/wallet", walletResponse);
			log.info("Broadcasted wallet update to user: {}", wallet.getUserId());
		} catch (Exception e) {
			log.error("Failed to broadcast wallet update for user {}: {}", wallet.getUserId(), e.getMessage());
		}
	}

}

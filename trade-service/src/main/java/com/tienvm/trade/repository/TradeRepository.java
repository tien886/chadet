package com.tienvm.trade.repository;

import java.util.List;
import java.util.UUID;

import com.tienvm.trade.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

	List<Trade> findByConversationIdOrderByCreatedAtDesc(UUID conversationId);

	List<Trade> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(UUID senderId, UUID receiverId);

}

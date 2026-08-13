package com.tienvm.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tienvm.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

	Page<Message> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

	List<Message> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);

	Optional<Message> findTop1ByConversation_IdOrderByCreatedAtDesc(UUID conversationId);

}

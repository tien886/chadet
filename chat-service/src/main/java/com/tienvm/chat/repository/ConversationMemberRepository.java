package com.tienvm.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tienvm.chat.entity.ConversationMember;
import com.tienvm.chat.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

	boolean existsByIdConversationIdAndIdUserId(UUID conversationId, UUID userId);

	List<ConversationMember> findByIdConversationId(UUID conversationId);

	Optional<ConversationMember> findByIdConversationIdAndIdUserId(UUID conversationId, UUID userId);

	void deleteByIdConversationIdAndIdUserId(UUID conversationId, UUID userId);

}

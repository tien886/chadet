package com.tienvm.chat.repository;

import java.util.UUID;

import com.tienvm.chat.entity.GroupChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, UUID> {
}

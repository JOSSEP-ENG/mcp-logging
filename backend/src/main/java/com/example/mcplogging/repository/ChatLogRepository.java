package com.example.mcplogging.repository;

import com.example.mcplogging.entity.ChatLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
    Page<ChatLog> findByUserId(String userId, Pageable pageable);

    Page<ChatLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}

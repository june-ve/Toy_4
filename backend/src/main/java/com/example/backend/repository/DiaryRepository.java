package com.example.backend.repository;

import com.example.backend.entity.Diary;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);
    List<Diary> findByUser_UserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
    boolean existsByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);
    List<Diary> findByUserOrderByCreatedAtDesc(User user);
    
    // 같은 날짜의 일기를 중복 제거하여 카운트
    @Query("SELECT COUNT(DISTINCT DATE(d.createdAt)) FROM Diary d WHERE d.user = :user")
    int countDistinctDatesByUser(@Param("user") User user);
} 
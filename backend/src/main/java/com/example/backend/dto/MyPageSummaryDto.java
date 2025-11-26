package com.example.backend.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 마이페이지 요약 정보를 담는 DTO 클래스
 */
@Getter
@AllArgsConstructor
@Builder
public class MyPageSummaryDto {
    private String nickname;
    private String email;
    private LocalDate joinDate;
    private int totalDiaryCount;
    private int consecutiveDiaryDays;
    private List<String> mainEmotions;
    private String recentAiComment;
    private String recentStampImage;
    private int commentTime;
    private int point;
} 
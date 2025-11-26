package com.example.backend.service;

import com.example.backend.dto.MyPageSummaryDto;
import com.example.backend.dto.UserStampDto;
import com.example.backend.entity.*;
import com.example.backend.repository.CommentEmotionMappingRepository;
import com.example.backend.repository.DailyCommentRepository;
import com.example.backend.repository.DiaryRepository;
import com.example.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {
    
    private static final String DEFAULT_STAMP_IMAGE = "image/default_stamp.png";
    private final DiaryRepository diaryRepository;
    private final DailyCommentRepository dailyCommentRepository;
    private final CommentEmotionMappingRepository commentEmotionMappingRepository;
    private final UserRepository userRepository;
    private final PointshopService pointshopService;

    // 마이페이지 요약 정보 조회
    @Transactional(readOnly = true)
    public MyPageSummaryDto getMyPageSummary(User user) {
        int totalDiaryCount = diaryRepository.countDistinctDatesByUser(user);
        int consecutiveDiaryDays = calculateConsecutiveDiaryDays(user);
        DailyComment recentComment = dailyCommentRepository.findTopByUserOrderByCreatedAtDesc(user);
        List<String> mainEmotionTags = extractEmotionTags(recentComment);
        String recentCommentContent = recentComment != null ? recentComment.getContent() : null;
        String recentStampImage = getActiveStampImage(user);

        MyPageSummaryDto dto = new MyPageSummaryDto();
        dto.setNickname(user.getUserNickname());
        dto.setEmail(user.getUserEmail());
        dto.setJoinDate(user.getUserCreatedAt().toLocalDate());
        dto.setTotalDiaryCount(totalDiaryCount);
        dto.setConsecutiveDiaryDays(consecutiveDiaryDays);
        dto.setMainEmotions(mainEmotionTags);
        dto.setRecentAiComment(recentCommentContent != null ? recentCommentContent : "AI 코멘트가 없습니다.");
        dto.setRecentStampImage(recentStampImage);
        dto.setCommentTime(user.getUserCommentTime());
        return dto;
    }

    // '주요 감정 상태' 태그 추출
    private List<String> extractEmotionTags(DailyComment dailyComment) {
        if (dailyComment == null) {
            return List.of();
        }

        return commentEmotionMappingRepository.findByDailyCommentIn(List.of(dailyComment)).stream()
            .map(CommentEmotionMapping::getEmotionData)
            .filter(java.util.Objects::nonNull)
            .map(EmotionData::getName)
            .filter(name -> name != null && !name.isBlank())
            .map(name -> "#" + name)
            .toList();
    }

    // 현재 적용된 스탬프 이미지 가져오기
    private String getActiveStampImage(User user) {
        try {
            UserStampDto activeStamp = pointshopService.getActiveStamp(user.getUserId());
            if (activeStamp != null && activeStamp.getStampImage() != null && !activeStamp.getStampImage().isBlank()) {
                return activeStamp.getStampImage();
            }
        } catch (RuntimeException e) {
            log.warn("Failed to load active stamp for user {}", user.getUserId(), e);
        }
        return DEFAULT_STAMP_IMAGE;
    }

    // 어제까지 연속 일기 작성 일수 계산
    private int calculateConsecutiveDiaryDays(User user) {
        List<Diary> diaries = diaryRepository.findByUserOrderByCreatedAtDesc(user);
        if (diaries.isEmpty()) {
            return 0;
        }
        java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
        int streak = 0;
        for (Diary diary : diaries) {
            java.time.LocalDate diaryDate = diary.getCreatedAt().toLocalDate();
            if (diaryDate.equals(yesterday.minusDays(streak))) {
                streak++;
            } else if (diaryDate.isBefore(yesterday.minusDays(streak))) {
                break;
            }
        }
        return streak;
    }

    // 코멘트 받을 시간 업데이트
    @Transactional
    public void updateCommentTime(User user, int commentHour) {
        user.setUserCommentTime(commentHour);
        userRepository.save(user);
    }
}

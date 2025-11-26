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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        return MyPageSummaryDto.builder()
            .nickname(user.getUserNickname())
            .email(user.getUserEmail())
            .joinDate(user.getUserCreatedAt().toLocalDate())
            .totalDiaryCount(totalDiaryCount)
            .consecutiveDiaryDays(consecutiveDiaryDays)
            .mainEmotions(mainEmotionTags)
            .recentAiComment(recentCommentContent != null ? recentCommentContent : "AI 코멘트가 없습니다.")
            .recentStampImage(recentStampImage)
            .commentTime(user.getUserCommentTime())
            .build();
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
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX);
        int streak = 0;

        // 1. 어제 일기가 없으면 연속 일수는 0
        boolean hasYesterdayDiary = diaryRepository.existsByUserAndCreatedAtBetween(user, startOfYesterday, endOfYesterday);
        if (!hasYesterdayDiary) {
            return streak;
        }

        // 2. 어제 일기가 있다면 전체 목록을 조회하여 연속 일수 계산
        List<Diary> diaries = diaryRepository.findByUserOrderByCreatedAtDesc(user);
        for (Diary diary : diaries) {
            LocalDate diaryDate = diary.getCreatedAt().toLocalDate();
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

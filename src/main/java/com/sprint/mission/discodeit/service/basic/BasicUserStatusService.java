package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.dto.userstatus.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.userstatus.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.exception.userstatus.UserStatusAlreadyExistsException;
import com.sprint.mission.discodeit.exception.userstatus.UserStatusNotFoundException;
import com.sprint.mission.discodeit.mapper.UserStatusMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserStatusService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicUserStatusService implements UserStatusService {

    private final UserStatusRepository userStatusRepository;
    private final UserRepository userRepository;
    private final UserStatusMapper userStatusMapper;

    @Transactional
    @Override
    public UserStatusDto create(UserStatusCreateRequest request) {
        UUID userId = request.userId();
        log.debug("[USER_STATUS_CREATE] 사용자 상태 생성 시작: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[USER_STATUS_CREATE] 사용자 상태 생성 실패 - 사용자를 찾을 수 없음: userId={}",
                            userId);
                    return new UserNotFoundException(userId);
                });
        Optional.ofNullable(user.getStatus())
                .ifPresent(status -> {
                    log.warn("[USER_STATUS_CREATE] 사용자 상태 생성 실패 - 이미 존재함: userId={}", userId);
                    throw new UserStatusAlreadyExistsException(userId);
                });

        Instant lastActiveAt = request.lastActiveAt();
        UserStatus userStatus = new UserStatus(user, lastActiveAt);
        userStatusRepository.save(userStatus);

        log.info("[USER_STATUS_CREATE] 사용자 상태 생성 완료: userStatusId={}", userStatus.getId());
        return userStatusMapper.toDto(userStatus);
    }

    @Override
    public UserStatusDto find(UUID userStatusId) {
        log.debug("[USER_STATUS_FIND] 사용자 상태 조회: userStatusId={}", userStatusId);
        return userStatusRepository.findById(userStatusId)
                .map(userStatusMapper::toDto)
                .orElseThrow(() -> UserStatusNotFoundException.byUserStatusId(userStatusId));
    }

    @Override
    public List<UserStatusDto> findAll() {
        log.debug("[USER_STATUS_FIND_ALL] 사용자 상태 목록 조회");
        return userStatusRepository.findAll().stream()
                .map(userStatusMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public UserStatusDto update(UUID userStatusId, UserStatusUpdateRequest request) {
        Instant newLastActiveAt = request.newLastActiveAt();
        log.debug("[USER_STATUS_UPDATE] 사용자 상태 수정 시작: userStatusId={}", userStatusId);

        UserStatus userStatus = userStatusRepository.findById(userStatusId)
                .orElseThrow(() -> {
                    log.warn("[USER_STATUS_UPDATE] 사용자 상태 수정 실패 - 사용자 상태를 찾을 수 없음: userStatusId={}",
                            userStatusId);
                    return UserStatusNotFoundException.byUserStatusId(userStatusId);
                });
        userStatus.update(newLastActiveAt);

        log.info("[USER_STATUS_UPDATE] 사용자 상태 수정 완료: userStatusId={}", userStatusId);
        return userStatusMapper.toDto(userStatus);
    }

    @Transactional
    @Override
    public UserStatusDto updateByUserId(UUID userId, UserStatusUpdateRequest request) {
        Instant newLastActiveAt = request.newLastActiveAt();
        log.debug("[USER_STATUS_UPDATE] 사용자 기준 상태 수정 시작: userId={}", userId);

        UserStatus userStatus = userStatusRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("[USER_STATUS_UPDATE] 사용자 기준 상태 수정 실패 - 사용자 상태를 찾을 수 없음: userId={}",
                            userId);
                    return UserStatusNotFoundException.byUserId(userId);
                });
        userStatus.update(newLastActiveAt);

        log.info("[USER_STATUS_UPDATE] 사용자 기준 상태 수정 완료: userId={}, userStatusId={}",
                userId, userStatus.getId());
        return userStatusMapper.toDto(userStatus);
    }

    @Transactional
    @Override
    public void delete(UUID userStatusId) {
        log.debug("[USER_STATUS_DELETE] 사용자 상태 삭제 시작: userStatusId={}", userStatusId);

        if (!userStatusRepository.existsById(userStatusId)) {
            log.warn("[USER_STATUS_DELETE] 사용자 상태 삭제 실패 - 사용자 상태를 찾을 수 없음: userStatusId={}",
                    userStatusId);
            throw UserStatusNotFoundException.byUserStatusId(userStatusId);
        }
        userStatusRepository.deleteById(userStatusId);
        log.info("[USER_STATUS_DELETE] 사용자 상태 삭제 완료: userStatusId={}", userStatusId);
    }
}

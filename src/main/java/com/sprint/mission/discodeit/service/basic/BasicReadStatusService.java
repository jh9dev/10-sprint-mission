package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.readstatus.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.readstatus.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ReadStatusDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.channel.ChannelNotFoundException;
import com.sprint.mission.discodeit.exception.readstatus.ReadStatusAlreadyExistsException;
import com.sprint.mission.discodeit.exception.readstatus.ReadStatusNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.mapper.ReadStatusMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ReadStatusService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicReadStatusService implements ReadStatusService {

    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ReadStatusMapper readStatusMapper;

    @Transactional
    @Override
    public ReadStatusDto create(ReadStatusCreateRequest request) {
        UUID userId = request.userId();
        UUID channelId = request.channelId();

        log.debug("[READ_STATUS_CREATE] 읽음 상태 생성 시작: userId={}, channelId={}",
                userId, channelId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[READ_STATUS_CREATE] 읽음 상태 생성 실패 - 사용자를 찾을 수 없음: userId={}",
                            userId);
                    return new UserNotFoundException(userId);
                });
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> {
                    log.warn("[READ_STATUS_CREATE] 읽음 상태 생성 실패 - 채널을 찾을 수 없음: channelId={}",
                            channelId);
                    return new ChannelNotFoundException(channelId);
                });

        if (readStatusRepository.findByUserIdAndChannelId(user.getId(), channel.getId())
                .isPresent()) {
            log.warn("[READ_STATUS_CREATE] 읽음 상태 생성 실패 - 이미 존재함: userId={}, channelId={}",
                    userId, channelId);
            throw new ReadStatusAlreadyExistsException(userId, channelId);
        }

        Instant lastReadAt = request.lastReadAt();
        ReadStatus readStatus = readStatusRepository.save(
                new ReadStatus(user, channel, lastReadAt));

        log.info("[READ_STATUS_CREATE] 읽음 상태 생성 완료: readStatusId={}", readStatus.getId());
        return readStatusMapper.toDto(readStatus);
    }

    @Override
    public ReadStatusDto find(UUID readStatusId) {
        log.debug("[READ_STATUS_FIND] 읽음 상태 조회: readStatusId={}", readStatusId);
        return readStatusRepository.findById(readStatusId)
                .map(readStatusMapper::toDto)
                .orElseThrow(() -> new ReadStatusNotFoundException(readStatusId));
    }

    @Override
    public List<ReadStatusDto> findAllByUserId(UUID userId) {
        log.debug("[READ_STATUS_FIND_ALL] 읽음 상태 목록 조회: userId={}", userId);
        return readStatusRepository.findAllByUserId(userId).stream()
                .map(readStatusMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ReadStatusDto update(UUID readStatusId, ReadStatusUpdateRequest request) {
        Instant newLastReadAt = request.newLastReadAt();
        log.debug("[READ_STATUS_UPDATE] 읽음 상태 수정 시작: readStatusId={}", readStatusId);

        ReadStatus readStatus = readStatusRepository.findById(readStatusId)
                .orElseThrow(() -> {
                    log.warn("[READ_STATUS_UPDATE] 읽음 상태 수정 실패 - 읽음 상태를 찾을 수 없음: readStatusId={}",
                            readStatusId);
                    return new ReadStatusNotFoundException(readStatusId);
                });
        readStatus.update(newLastReadAt);

        log.info("[READ_STATUS_UPDATE] 읽음 상태 수정 완료: readStatusId={}", readStatusId);
        return readStatusMapper.toDto(readStatus);
    }

    @Transactional
    @Override
    public void delete(UUID readStatusId) {
        log.debug("[READ_STATUS_DELETE] 읽음 상태 삭제 시작: readStatusId={}", readStatusId);

        if (!readStatusRepository.existsById(readStatusId)) {
            log.warn("[READ_STATUS_DELETE] 읽음 상태 삭제 실패 - 읽음 상태를 찾을 수 없음: readStatusId={}",
                    readStatusId);
            throw new ReadStatusNotFoundException(readStatusId);
        }
        readStatusRepository.deleteById(readStatusId);
        log.info("[READ_STATUS_DELETE] 읽음 상태 삭제 완료: readStatusId={}", readStatusId);
    }
}

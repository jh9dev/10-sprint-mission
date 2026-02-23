package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.readstatus.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.readstatus.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.exception.BusinessException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ReadStatusService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BasicReadStatusService implements ReadStatusService {

  private final ReadStatusRepository readStatusRepository;
  private final UserRepository userRepository;
  private final ChannelRepository channelRepository;

  @Override
  public ReadStatus create(ReadStatusCreateRequest request) {
    UUID userId = request.userId();
    UUID channelId = request.channelId();

    // 채널과 유저 존재 여부
    if (!userRepository.existsById(userId)) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    if (!channelRepository.existsById(channelId)) {
      throw new BusinessException(ErrorCode.CHANNEL_NOT_FOUND);
    }

    // 유저가 해당 채널에 대한 메시지 읽음 상태가 이미 존재하면 반환하고, 없으면 새로 생성해서 저장 후 반환
    return readStatusRepository.findAllByUserId(userId).stream()
        .filter(readStatus -> readStatus.getChannelId().equals(channelId))
        .findFirst()
        .orElseGet(
            () -> {
              Instant lastReadAt = request.lastReadAt();
              ReadStatus readStatus = new ReadStatus(userId, channelId, lastReadAt);
              return readStatusRepository.save(readStatus);
            }
        );
  }

  @Override
  public ReadStatus find(UUID readStatusId) {
    return readStatusRepository.findById(readStatusId)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.READ_STATUS_NOT_FOUND));
  }

  @Override
  public List<ReadStatus> findAllByUserId(UUID userId) {
    return readStatusRepository.findAllByUserId(userId).stream()
        .toList();
  }

  @Override
  public ReadStatus update(UUID readStatusId, ReadStatusUpdateRequest request) {
    Instant newLastReadAt = request.newLastReadAt();
    // 메시지 읽음 상태 조회
    ReadStatus readStatus = readStatusRepository.findById(readStatusId)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.READ_STATUS_NOT_FOUND));
    readStatus.update(newLastReadAt);
    return readStatusRepository.save(readStatus);
  }

  @Override
  public void delete(UUID readStatusId) {
    if (!readStatusRepository.existsById(readStatusId)) {
      throw new BusinessException(ErrorCode.READ_STATUS_NOT_FOUND);
    }
    readStatusRepository.deleteById(readStatusId);
  }
}

package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.channel.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.BusinessException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class BasicChannelService implements ChannelService {

  private final ChannelRepository channelRepository;
  private final UserRepository userRepository;
  private final ReadStatusRepository readStatusRepository;
  private final MessageRepository messageRepository;
  private final ChannelMapper channelMapper;

  @Override
  public ChannelDto create(PublicChannelCreateRequest publicChannelCreateRequest) {
    String name = publicChannelCreateRequest.name();
    String description = publicChannelCreateRequest.description();
    Channel channel = new Channel(name, description, ChannelType.PUBLIC);

    channelRepository.save(channel);

    return channelMapper.toDto(channel);
  }

  @Override
  public ChannelDto create(PrivateChannelCreateRequest privateChannelCreateRequest) {
    Channel channel = channelRepository.save(new Channel(null, null, ChannelType.PRIVATE));

    List<UUID> participantIds = privateChannelCreateRequest.participantIds();
    List<User> users = userRepository.findAllById(participantIds);

    if (participantIds.size() != users.size()) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    List<ReadStatus> readStatuses = users.stream()
        .map(u -> new ReadStatus(u, channel, channel.getCreatedAt()))
        .toList();

    readStatusRepository.saveAll(readStatuses);

    return channelMapper.toDto(channel);
  }

  @Transactional(readOnly = true)
  @Override
  public ChannelDto find(UUID channelId) {
    return channelRepository.findById(channelId)
        .map(channelMapper::toDto)
        .orElseThrow(() -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  @Override
  public List<ChannelDto> findAllByUserId(UUID userId) {
    // 유저의 참여 채널 목록 조회
    List<UUID> mySubscribedChannelIds = readStatusRepository.findAllByUserId(userId).stream()
        .map(r -> r.getChannel().getId())
        .toList();

    // 공개 채널이거나 유저가 참여하고 있는 채널의 목록 반환
    return channelRepository.findAll().stream()
        .filter(c ->
            c.getType().equals(ChannelType.PUBLIC)
                || mySubscribedChannelIds.contains(c.getId())
        )
        .map(channelMapper::toDto)
        .toList();
  }

  @Override
  public ChannelDto update(UUID channelId, PublicChannelUpdateRequest publicChannelUpdateRequest) {
    String newName = publicChannelUpdateRequest.newName();
    String newDescription = publicChannelUpdateRequest.newDescription();

    // 채널 조회
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));

    // 비공개 채널이라면 수정 불가
    if (channel.getType().equals(ChannelType.PRIVATE)) {
      throw new BusinessException(ErrorCode.PRIVATE_CHANNEL_UPDATE_NOT_ALLOWED);
    }

    channel.update(newName, newDescription);

    return channelMapper.toDto(channel);
  }

  @Override
  public void delete(UUID channelId) {
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));

    // 해당 채널의 메시지와 메시지 읽음 상태 삭제
    messageRepository.deleteAllByChannelId(channel.getId());
    readStatusRepository.deleteAllByChannelId(channel.getId());

    channelRepository.deleteById(channelId);
  }
}

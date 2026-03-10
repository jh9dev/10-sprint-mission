package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.channel.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.BusinessException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.mapper.ChannelMapper;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.ChannelService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final UserMapper userMapper;

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

    channel.getReadStatuses().addAll(readStatuses);
    readStatusRepository.saveAll(readStatuses);

    return buildChannelDto(List.of(channel)).get(0);
  }

  @Transactional(readOnly = true)
  @Override
  public ChannelDto find(UUID channelId) {
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));
    return buildChannelDto(List.of(channel)).get(0);
  }

  @Transactional(readOnly = true)
  @Override
  public List<ChannelDto> findAllByUserId(UUID userId) {
    return buildChannelDto(channelRepository.findAllVisibleByUserId(userId));
  }

  @Override
  public ChannelDto update(UUID channelId, PublicChannelUpdateRequest publicChannelUpdateRequest) {
    String newName = publicChannelUpdateRequest.newName();
    String newDescription = publicChannelUpdateRequest.newDescription();

    // 채널 조회
    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));

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
        .orElseThrow(() -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));

    // 해당 채널의 메시지와 메시지 읽음 상태 삭제
    messageRepository.deleteAllByChannelId(channel.getId());
    readStatusRepository.deleteAllByChannelId(channel.getId());

    channelRepository.deleteById(channelId);
  }

  private List<ChannelDto> buildChannelDto(List<Channel> channels) {
    if (channels.isEmpty()) {
      return List.of();
    }

    List<UUID> channelIds = channels.stream()
        .map(Channel::getId)
        .toList();

    Map<UUID, Instant> lastMessageAtByChannelId = messageRepository
        .findLastMessageAtByChannelIdIn(channelIds).stream()
        .collect(Collectors.toMap(
            MessageRepository.ChannelLastMessageAtProjection::getChannelId,
            MessageRepository.ChannelLastMessageAtProjection::getLastMessageAt
        ));

    List<UUID> privateChannelIds = channels.stream()
        .filter(channel -> ChannelType.PRIVATE.equals(channel.getType()))
        .map(Channel::getId)
        .toList();

    Map<UUID, List<UserDto>> participantsByChannelId = buildParticipantsByChannelId(
        privateChannelIds);

    return channels.stream()
        .map(channel -> new ChannelDto(
            channel.getId(),
            channel.getType(),
            channel.getName(),
            channel.getDescription(),
            participantsByChannelId.getOrDefault(channel.getId(), List.of()),
            lastMessageAtByChannelId.get(channel.getId())
        ))
        .toList();
  }

  private Map<UUID, List<UserDto>> buildParticipantsByChannelId(List<UUID> privateChannelIds) {
    if (privateChannelIds.isEmpty()) {
      return Map.of();
    }

    return readStatusRepository.findAllByChannelIdInWithUser(privateChannelIds).stream()
        .collect(Collectors.groupingBy(
            readStatus -> readStatus.getChannel().getId(),
            LinkedHashMap::new,
            Collectors.mapping(readStatus -> userMapper.toDto(readStatus.getUser()),
                Collectors.toList())
        ));
  }
}
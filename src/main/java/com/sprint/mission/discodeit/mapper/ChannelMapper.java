package com.sprint.mission.discodeit.mapper;

import com.sprint.mission.discodeit.dto.response.ChannelDto;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface ChannelMapper {

  @Mapping(target = "participants", expression = "java(mapParticipants(channel))")
  @Mapping(target = "lastMessageAt", expression = "java(mapLastMessageAt(channel))")
  ChannelDto toDto(Channel channel);

  default Instant mapLastMessageAt(Channel channel) {
    return channel.getMessages().stream()
        .map(Message::getCreatedAt)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  default List<UserDto> mapParticipants(Channel channel) {
    if (!ChannelType.PRIVATE.equals(channel.getType())) {
      return List.of();
    }

    return channel.getReadStatuses().stream()
        .map(ReadStatus::getUser)
        .map(this::mapUser)
        .toList();
  }

  UserDto mapUser(User user);
}

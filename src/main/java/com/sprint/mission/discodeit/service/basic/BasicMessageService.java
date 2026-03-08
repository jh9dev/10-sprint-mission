package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.BusinessException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class BasicMessageService implements MessageService {

  private final MessageRepository messageRepository;
  private final ChannelRepository channelRepository;
  private final UserRepository userRepository;
  private final BinaryContentRepository binaryContentRepository;
  private final MessageMapper messageMapper;

  @Override
  public MessageDto create(MessageCreateRequest messageCreateRequest,
      List<BinaryContentCreateRequest> binaryContentCreateRequests) {
    // 채널 검색
    Channel channel = channelRepository.findById(messageCreateRequest.channelId())
        .orElseThrow(() -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));

    // 유저 검색
    User author = userRepository.findById(messageCreateRequest.authorId())
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 바이너리 컨텐츠 생성
    List<BinaryContent> attachments = binaryContentCreateRequests.stream()
        .map(a -> new BinaryContent(
            a.fileName(),
            (long) a.bytes().length,
            a.contentType(),
            a.bytes()
        ))
        .toList();

    List<BinaryContent> savedAttachments = binaryContentRepository.saveAll(attachments);

    Message message = new Message(messageCreateRequest.content(), channel, author);
    messageRepository.save(message);

    return messageMapper.toDto(message);
  }

  @Transactional(readOnly = true)
  @Override
  public MessageDto find(UUID messageId) {
    return messageRepository.findById(messageId)
        .map(messageMapper::toDto)
        .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  @Override
  public List<MessageDto> findAllByChannelId(UUID channelId) {
    return messageRepository.findAllByChannelId(channelId).stream()
        .map(messageMapper::toDto)
        .toList();
  }

  @Override
  public MessageDto update(UUID messageId, MessageUpdateRequest messageUpdateRequest) {
    String newContent = messageUpdateRequest.newContent();
    // 메시지 조회
    Message message = messageRepository.findById(messageId)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

    message.update(newContent);

    return messageMapper.toDto(message);
  }

  @Override
  public void delete(UUID messageId) {
    // 메시지 조회
    Message message = messageRepository.findById(messageId)
        .orElseThrow(() -> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));

    messageRepository.delete(message);
  }
}

package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.dto.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.BusinessException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class BasicMessageService implements MessageService {

  private static final int MESSAGE_PAGE_SIZE = 50;
  private static final Sort MESSAGE_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

  private final MessageRepository messageRepository;
  private final ChannelRepository channelRepository;
  private final UserRepository userRepository;
  private final BinaryContentRepository binaryContentRepository;
  private final BinaryContentStorage binaryContentStorage;
  private final MessageMapper messageMapper;
  private final PageResponseMapper pageResponseMapper;

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
        .map(attachment -> {
          String fileName = attachment.fileName();
          String contentType = attachment.contentType();
          byte[] bytes = attachment.bytes();
          BinaryContent binaryContent = new BinaryContent(
              fileName,
              (long) bytes.length,
              contentType);
          binaryContentRepository.save(binaryContent);
          binaryContentStorage.put(binaryContent.getId(), bytes);
          return binaryContent;
        })
        .toList();

    Message message = new Message(messageCreateRequest.content(), channel, author);
    message.setAttachments(attachments);
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
  public PageResponse<MessageDto> findAllByChannelId(UUID channelId, Pageable pageable) {
    Slice<MessageDto> messageSlice = messageRepository.findByChannelId(channelId,
            normalizePageable(pageable))
        .map(messageMapper::toDto);
    return pageResponseMapper.fromSlice(messageSlice);
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

  private Pageable normalizePageable(Pageable pageable) {
    int pageNumber = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
    return PageRequest.of(pageNumber, MESSAGE_PAGE_SIZE, MESSAGE_SORT);
  }
}

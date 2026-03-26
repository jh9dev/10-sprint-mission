package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicMessageService implements MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentRepository binaryContentRepository;
    private final PageResponseMapper pageResponseMapper;

    @Transactional
    @Override
    public MessageDto create(MessageCreateRequest messageCreateRequest,
            List<BinaryContentCreateRequest> binaryContentCreateRequests) {
        UUID channelId = messageCreateRequest.channelId();
        UUID authorId = messageCreateRequest.authorId();
        int attachmentCount = binaryContentCreateRequests.size();

        log.debug("[MESSAGE_CREATE] 메시지 생성 시작: channelId={}, authorId={}, attachmentCount={}",
                channelId, authorId, attachmentCount);

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(
                        () -> {
                            log.warn("[MESSAGE_CREATE] 메시지 생성 실패 - 채널 없음: channelId={}", channelId);
                            return new NoSuchElementException(
                                    "Channel with id " + channelId + " does not exist");
                        });
        User author = userRepository.findById(authorId)
                .orElseThrow(
                        () -> {
                            log.warn("[MESSAGE_CREATE] 메시지 생성 실패 - 작성자 없음: authorId={}", authorId);
                            return new NoSuchElementException(
                                    "Author with id " + authorId + " does not exist");
                        });

        try {
            List<BinaryContent> attachments = binaryContentCreateRequests.stream()
                    .map(attachmentRequest -> {
                        String fileName = attachmentRequest.fileName();
                        String contentType = attachmentRequest.contentType();
                        byte[] bytes = attachmentRequest.bytes();

                        BinaryContent binaryContent = new BinaryContent(fileName,
                                (long) bytes.length,
                                contentType);
                        binaryContentRepository.save(binaryContent);
                        binaryContentStorage.put(binaryContent.getId(), bytes);
                        return binaryContent;
                    })
                    .toList();

            String content = messageCreateRequest.content();
            Message message = new Message(
                    content,
                    channel,
                    author,
                    attachments
            );

            messageRepository.save(message);

            log.info("[MESSAGE_CREATE] 메시지 생성 완료: messageId={}, channelId={}, authorId={}",
                    message.getId(), channelId, authorId);
            return messageMapper.toDto(message);
        } catch (Exception e) {
            log.error("[MESSAGE_CREATE] 메시지 생성 중 예외 발생: channelId={}, authorId={}", channelId,
                    authorId, e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public MessageDto find(UUID messageId) {
        return messageRepository.findById(messageId)
                .map(messageMapper::toDto)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "Message with id " + messageId + " not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<MessageDto> findAllByChannelId(UUID channelId, Instant createAt,
            Pageable pageable) {
        Slice<MessageDto> slice = messageRepository.findAllByChannelIdWithAuthor(channelId,
                        Optional.ofNullable(createAt).orElse(Instant.now()),
                        pageable)
                .map(messageMapper::toDto);

        Instant nextCursor = null;
        if (!slice.getContent().isEmpty()) {
            nextCursor = slice.getContent().get(slice.getContent().size() - 1)
                    .createdAt();
        }

        return pageResponseMapper.fromSlice(slice, nextCursor);
    }

    @Transactional
    @Override
    public MessageDto update(UUID messageId, MessageUpdateRequest request) {
        String newContent = request.newContent();
        log.debug("[MESSAGE_UPDATE] 메시지 수정 시작: messageId={}", messageId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(
                        () -> {
                            log.warn("[MESSAGE_UPDATE] 메시지 수정 실패 - 메시지 없음: messageId={}",
                                    messageId);
                            return new NoSuchElementException(
                                    "Message with id " + messageId + " not found");
                        });

        try {
            message.update(newContent);

            log.info("[MESSAGE_UPDATE] 메시지 수정 완료: messageId={}", messageId);
            return messageMapper.toDto(message);
        } catch (Exception e) {
            log.error("[MESSAGE_UPDATE] 메시지 수정 중 예외 발생: messageId={}", messageId, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(UUID messageId) {
        log.debug("[MESSAGE_DELETE] 메시지 삭제 시작: messageId={}", messageId);

        if (!messageRepository.existsById(messageId)) {
            log.warn("[MESSAGE_DELETE] 메시지 삭제 실패 - 메시지 없음: messageId={}", messageId);
            throw new NoSuchElementException("Message with id " + messageId + " not found");
        }

        try {
            messageRepository.deleteById(messageId);

            log.info("[MESSAGE_DELETE] 메시지 삭제 완료: messageId={}", messageId);
        } catch (Exception e) {
            log.error("[MESSAGE_DELETE] 메시지 삭제 중 예외 발생: messageId={}", messageId, e);
            throw e;
        }
    }
}

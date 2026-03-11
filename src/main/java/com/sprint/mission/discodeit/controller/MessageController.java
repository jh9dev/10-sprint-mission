package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageDto;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.service.MessageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/messages")
public class MessageController {

  private final MessageService messageService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MessageDto> create(
      @Valid @RequestPart("messageCreateRequest") MessageCreateRequest messageCreateRequest,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments
  ) {

    List<BinaryContentCreateRequest> attachmentRequests =
        Optional.ofNullable(attachments)
            .map(files -> files.stream()
                .map(this::toBinaryContentCreateRequest)
                .toList())
            .orElse(List.of());

    MessageDto createdMessage =
        messageService.create(messageCreateRequest, attachmentRequests);

    return ResponseEntity.status(HttpStatus.CREATED).body(createdMessage);
  }

  @GetMapping
  public ResponseEntity<PageResponse<MessageDto>> findAllByChannelId(
      @RequestParam("channelId") UUID channelId,
      @RequestParam(value = "cursor", required = false) Instant cursor,
      @ParameterObject
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable
  ) {

    PageResponse<MessageDto> messages =
        messageService.findAllByChannelId(channelId, cursor, pageable);

    return ResponseEntity.status(HttpStatus.OK).body(messages);
  }

  @PatchMapping(path = "/{messageId}")
  public ResponseEntity<MessageDto> update(
      @PathVariable UUID messageId,
      @Valid @RequestBody MessageUpdateRequest request
  ) {

    MessageDto updatedMessage =
        messageService.update(messageId, request);

    return ResponseEntity.status(HttpStatus.OK).body(updatedMessage);
  }

  @DeleteMapping(path = "/{messageId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID messageId
  ) {

    messageService.delete(messageId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  private BinaryContentCreateRequest toBinaryContentCreateRequest(MultipartFile file) {

    String fileName = file.getOriginalFilename();
    String contentType = file.getContentType();

    if (fileName == null || fileName.isBlank()) {
      throw new IllegalArgumentException("파일 이름이 올바르지 않습니다: attachments");
    }

    if (contentType == null || contentType.isBlank()) {
      throw new IllegalArgumentException("파일 형식을 확인할 수 없습니다: attachments");
    }

    try {
      return new BinaryContentCreateRequest(
          fileName,
          contentType,
          file.getBytes()
      );
    } catch (IOException e) {
      throw new IllegalArgumentException("멀티파트 파일을 읽을 수 없습니다: attachments");
    }
  }
}
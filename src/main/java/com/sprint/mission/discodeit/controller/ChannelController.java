package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.channel.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.channel.PublicChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.data.ChannelDto;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.exception.ErrorDto;
import com.sprint.mission.discodeit.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/channels")
public class ChannelController {

  private final ChannelService channelService;

  @Operation(summary = "공개 채널 생성")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201", description = "공개 채널 생성 성공",
          content = @Content(schema = @Schema(implementation = Channel.class))
      )
  })
  @PostMapping(path = "/public")
  public ResponseEntity<Channel> create(@Valid @RequestBody PublicChannelCreateRequest request) {
    Channel createdChannel = channelService.create(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdChannel);
  }

  @Operation(summary = "비공개 채널 생성")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201", description = "비공개 채널 생성 성공",
          content = @Content(schema = @Schema(implementation = Channel.class))
      )
  })
  @PostMapping(path = "/private")
  public ResponseEntity<Channel> create(@Valid @RequestBody PrivateChannelCreateRequest request) {
    Channel createdChannel = channelService.create(request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdChannel);
  }

  @Operation(summary = "유저가 참여 중인 채널 목록 조회")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "유저가 참여 중인 채널 목록 조회",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChannelDto.class)))
      )
  })
  @GetMapping
  public ResponseEntity<List<ChannelDto>> findAll(@RequestParam("userId") UUID userId) {
    List<ChannelDto> channels = channelService.findAllByUserId(userId);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(channels);
  }

  @Operation(summary = "공개 채널 정보 수정")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "공개 채널 정보 수정 성공",
          content = @Content(schema = @Schema(implementation = Channel.class))
      ),
      @ApiResponse(
          responseCode = "403", description = "비공개 채널은 수정 불가",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 403, \"error\": \"PRIVATE_CHANNEL_UPDATE_FORBIDDEN\", \"message\": \"비공개 채널은 수정할 수 없습니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "채널을 찾을 수 없음",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 404, \"error\": \"CHANNEL_NOT_FOUND\", \"message\": \"채널을 찾을 수 없습니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      )
  })
  @PatchMapping(path = "/{channelId}")
  public ResponseEntity<Channel> update(@PathVariable UUID channelId,
      @Valid @RequestBody PublicChannelUpdateRequest request) {
    Channel udpatedChannel = channelService.update(channelId, request);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(udpatedChannel);
  }

  @Operation(summary = "채널 삭제")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "204", description = "채널 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "404",
          description = "채널을 찾을 수 없음",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 404, \"error\": \"CHANNEL_NOT_FOUND\", \"message\": \"채널을 찾을 수 없습니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      )
  })
  @DeleteMapping(path = "/{channelId}")
  public ResponseEntity<Void> delete(@PathVariable UUID channelId) {
    channelService.delete(channelId);
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }
}

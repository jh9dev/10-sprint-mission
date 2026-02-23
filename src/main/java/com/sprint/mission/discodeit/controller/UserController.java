package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.user.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.userStatus.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.ErrorDto;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final UserStatusService userStatusService;

  @Operation(summary = "유저 생성")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201", description = "유저 생성 성공",
          content = @Content(schema = @Schema(implementation = User.class))
      ),
      @ApiResponse(
          responseCode = "409", description = "이미 존재하는 username 또는 email은 사용 불가",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 409, \"error\": \"EMAIL_DUPLICATED\", \"message\": \"사용 중인 이메일입니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      )
  })
  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  public ResponseEntity<User> create(
      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      @Valid @RequestPart("userCreateRequest") UserCreateRequest userCreateRequest,
      @RequestPart(value = "profile", required = false) MultipartFile profile
  ) {
    Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
        .flatMap(this::resolveProfileRequest);
    User createdUser = userService.create(userCreateRequest, profileRequest);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdUser);
  }

  @Operation(summary = "전체 유저 조회")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "전체 유저 조회 성공",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))
      )
  })
  @GetMapping
  public ResponseEntity<List<UserDto>> findAll() {
    List<UserDto> users = userService.findAll();
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(users);
  }

  @Operation(summary = "유저 정보 수정")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "유저 정보 수정 성공",
          content = @Content(schema = @Schema(implementation = User.class))
      ),
      @ApiResponse(
          responseCode = "404", description = "유저를 찾을 수 없음",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 404, \"error\": \"USER_NOT_FOUND\", \"message\": \"유저를 찾을 수 없습니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      ),
      @ApiResponse(
          responseCode = "409", description = "이미 존재하는 username 또는 email은 사용 불가",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 409, \"error\": \"EMAIL_DUPLICATED\", \"message\": \"사용 중인 이메일입니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      )
  })
  @PatchMapping(
      path = "/{userId}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
  )
  public ResponseEntity<User> update(
      @PathVariable UUID userId,
      @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
      @Valid @RequestPart("userUpdateRequest") UserUpdateRequest userUpdateRequest,
      @RequestPart(value = "profile", required = false) MultipartFile profile
  ) {
    Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
        .flatMap(this::resolveProfileRequest);
    User updatedUser = userService.update(userId, userUpdateRequest, profileRequest);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(updatedUser);
  }

  @Operation(summary = "유저 온라인 상태 업데이트")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "유저 온라인 상태 업데이트 성공",
          content = @Content(schema = @Schema(implementation = User.class))
      ),
      @ApiResponse(
          responseCode = "404", description = "유저 상태를 찾을 수 없음",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 404, \"error\": \"USER_STATUS_NOT_FOUND\", \"message\": \"유저 상태 정보를 찾을 수 없습니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      )
  })
  @PatchMapping(path = "/{userId}/userStatus")
  public ResponseEntity<UserStatus> updateUserStatusByUserId(@PathVariable UUID userId,
      @Valid @RequestBody UserStatusUpdateRequest request) {
    UserStatus updatedUserStatus = userStatusService.updateByUserId(userId, request);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(updatedUserStatus);
  }

  @Operation(summary = "유저 삭제")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "204", description = "유저 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "404", description = "유저를 찾을 수 없음",
          content = @Content(
              schema = @Schema(implementation = ErrorDto.class),
              examples = @ExampleObject(value = "{ \"status\": 404, \"error\": \"USER_NOT_FOUND\", \"message\": \"유저를 찾을 수 없습니다.\", \"time\": \"2026-02-23T05:23:49.657764500Z\" }")
          )
      )
  })
  @DeleteMapping(path = "/{userId}")
  public ResponseEntity<Void> delete(@PathVariable UUID userId) {
    userService.delete(userId);
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  private Optional<BinaryContentCreateRequest> resolveProfileRequest(MultipartFile profileFile) {
    if (profileFile.isEmpty()) {
      return Optional.empty();
    } else {
      try {
        BinaryContentCreateRequest binaryContentCreateRequest = new BinaryContentCreateRequest(
            profileFile.getOriginalFilename(),
            profileFile.getContentType(),
            profileFile.getBytes()
        );
        return Optional.of(binaryContentCreateRequest);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}

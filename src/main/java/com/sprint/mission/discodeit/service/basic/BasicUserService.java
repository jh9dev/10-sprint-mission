package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.user.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.exception.BusinessException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Service
public class BasicUserService implements UserService {

  private final UserRepository userRepository;
  private final BinaryContentRepository binaryContentRepository;
  private final BinaryContentStorage binaryContentStorage;
  private final UserStatusRepository userStatusRepository;
  private final UserMapper userMapper;

  @Override
  public UserDto create(UserCreateRequest userCreateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    String username = userCreateRequest.username();
    String email = userCreateRequest.email();

    // Email과 Username이 이미 존재하는지 확인
    if (userRepository.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
    if (userRepository.existsByUsername(username)) {
      throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    // 생성할 프로필이 존재한다면 생성, 아니면 null
    BinaryContent nullableProfile = optionalProfileCreateRequest
        .map(profileRequest -> {
          String fileName = profileRequest.fileName();
          String contentType = profileRequest.contentType();
          byte[] bytes = profileRequest.bytes();
          BinaryContent binaryContent = new BinaryContent(
              fileName,
              (long) bytes.length,
              contentType);
          binaryContentRepository.save(binaryContent);
          binaryContentStorage.put(binaryContent.getId(), bytes);
          return binaryContent;
        })
        .orElse(null);

    String password = userCreateRequest.password();

    // 유저 생성
    User user = new User(username, email, password, nullableProfile);
    User createdUser = userRepository.save(user);

    // 유저 상태 생성, 마지막 활동 시각은 현재 시각으로 저장
    UserStatus userStatus = new UserStatus(createdUser, Instant.now());
    createdUser.setUserStatus(userStatus);
    userStatusRepository.save(userStatus);

    return userMapper.toDto(createdUser);
  }

  @Transactional(readOnly = true)
  @Override
  public UserDto find(UUID userId) {
    return userRepository.findById(userId)
        .map(userMapper::toDto)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  @Override
  public List<UserDto> findAll() {
    return userRepository.findAll().stream()
        .map(userMapper::toDto)
        .toList();
  }

  @Override
  public UserDto update(UUID userId, UserUpdateRequest userUpdateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    // 유저 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    String newUsername = userUpdateRequest.newUsername();
    String newEmail = userUpdateRequest.newEmail();
    // 새로운 Email과 Username이 이미 존재하는지 확인
    if (newEmail != null && !newEmail.equals(user.getEmail()) && userRepository.existsByEmail(
        newEmail)) {
      throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
    if (newUsername != null && !newUsername.equals(user.getUsername())
        && userRepository.existsByUsername(newUsername)) {
      throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    // 생성할 프로필이 존재한다면 기존 프로필 삭제 후 생성, 아니면 null
    BinaryContent nullableProfile = optionalProfileCreateRequest
        .map(profileRequest -> {
          Optional.ofNullable(user.getProfile())
              .ifPresent(binaryContentRepository::delete);

          String fileName = profileRequest.fileName();
          String contentType = profileRequest.contentType();
          byte[] bytes = profileRequest.bytes();
          BinaryContent binaryContent = new BinaryContent(
              fileName,
              (long) bytes.length,
              contentType);
          binaryContentRepository.save(binaryContent);
          binaryContentStorage.put(binaryContent.getId(), bytes);
          return binaryContent;
        })
        .orElse(null);

    String newPassword = userUpdateRequest.newPassword();
    user.update(newUsername, newEmail, newPassword, nullableProfile);

    return userMapper.toDto(user);
  }

  @Override
  public void delete(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 프로필이 존재한다면 삭제
    Optional.ofNullable(user.getProfile())
        .ifPresent(binaryContentRepository::delete);
    userStatusRepository.deleteByUserId(userId);

    userRepository.delete(user);
  }
}

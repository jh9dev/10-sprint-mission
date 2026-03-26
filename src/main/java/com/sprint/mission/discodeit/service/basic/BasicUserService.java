package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.binarycontent.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.user.UserCreateRequest;
import com.sprint.mission.discodeit.dto.user.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicUserService implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BinaryContentRepository binaryContentRepository;
    private final BinaryContentStorage binaryContentStorage;

    @Transactional
    @Override
    public UserDto create(UserCreateRequest userCreateRequest,
            Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
        String username = userCreateRequest.username();
        String email = userCreateRequest.email();

        log.debug("[USER_CREATE] 사용자 생성 시작: email={}, username={}", email, username);

        if (userRepository.existsByEmail(email)) {
            log.warn("[USER_CREATE] 사용자 생성 실패 - 이메일 중복: email={}", email);
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }
        if (userRepository.existsByUsername(username)) {
            log.warn("[USER_CREATE] 사용자 생성 실패 - username 중복: username={}", username);
            throw new IllegalArgumentException(
                    "User with username " + username + " already exists");
        }
        try {
            BinaryContent nullableProfile = optionalProfileCreateRequest
                    .map(profileRequest -> {
                        String fileName = profileRequest.fileName();
                        String contentType = profileRequest.contentType();
                        byte[] bytes = profileRequest.bytes();
                        BinaryContent binaryContent = new BinaryContent(fileName,
                                (long) bytes.length,
                                contentType);
                        binaryContentRepository.save(binaryContent);
                        binaryContentStorage.put(binaryContent.getId(), bytes);
                        return binaryContent;
                    })
                    .orElse(null);
            String password = userCreateRequest.password();

            User user = new User(username, email, password, nullableProfile);
            Instant now = Instant.now();
            UserStatus userStatus = new UserStatus(user, now);

            userRepository.save(user);
            log.info("[USER_CREATE] 사용자 생성 완료: userId={}, email={}", user.getId(), user.getEmail());

            return userMapper.toDto(user);
        } catch (Exception e) {
            log.error("[USER_CREATE] 사용자 생성 중 예외 발생: email={}, username={}", email, username, e);
            throw e;
        }
    }

    @Override
    public UserDto find(UUID userId) {
        return userRepository.findById(userId)
                .map(userMapper::toDto)
                .orElseThrow(
                        () -> new NoSuchElementException("User with id " + userId + " not found"));
    }

    @Override
    public List<UserDto> findAll() {
        return userRepository.findAllWithProfileAndStatus()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public UserDto update(UUID userId, UserUpdateRequest userUpdateRequest,
            Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
        log.debug("[USER_UPDATE] 사용자 수정 시작: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> {
                            log.warn("[USER_UPDATE] 사용자 수정 실패 - 사용자 없음: userId={}", userId);
                            return new NoSuchElementException(
                                    "User with id " + userId + " not found");
                        });

        String newUsername = userUpdateRequest.newUsername();
        String newEmail = userUpdateRequest.newEmail();
        if (userRepository.existsByEmail(newEmail)) {
            log.warn("[USER_UPDATE] 사용자 수정 실패 - 이메일 중복: email={}", newEmail);
            throw new IllegalArgumentException("User with email " + newEmail + " already exists");
        }
        if (userRepository.existsByUsername(newUsername)) {
            log.warn("[USER_UPDATE] 사용자 수정 실패 - username 중복: username={}", newUsername);
            throw new IllegalArgumentException(
                    "User with username " + newUsername + " already exists");
        }

        try {
            BinaryContent nullableProfile = optionalProfileCreateRequest
                    .map(profileRequest -> {

                        String fileName = profileRequest.fileName();
                        String contentType = profileRequest.contentType();
                        byte[] bytes = profileRequest.bytes();
                        BinaryContent binaryContent = new BinaryContent(fileName,
                                (long) bytes.length,
                                contentType);
                        binaryContentRepository.save(binaryContent);
                        binaryContentStorage.put(binaryContent.getId(), bytes);
                        return binaryContent;
                    })
                    .orElse(null);

            String newPassword = userUpdateRequest.newPassword();
            user.update(newUsername, newEmail, newPassword, nullableProfile);

            log.info("[USER_UPDATE] 사용자 수정 완료. userId={}", userId);
            return userMapper.toDto(user);
        } catch (Exception e) {
            log.error("[USER_UPDATE] 사용자 수정 중 예외 발생: userId={}", userId, e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void delete(UUID userId) {
        log.debug("[USER_DELETE] 사용자 삭제 시작: userId={}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("[USER_DELETE] 사용자 삭제 실패 - 사용자 없음: userId={}", userId);
            throw new NoSuchElementException("User with id " + userId + " not found");
        }

        try {
            userRepository.deleteById(userId);
            log.info("[USER_DELETE] 사용자 삭제 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("[USER_DELETE] 사용자 삭제 중 예외 발생: userId={}", userId, e);
            throw e;
        }
    }
}

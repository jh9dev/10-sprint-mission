package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.auth.LoginRequest;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.BusinessException;
import com.sprint.mission.discodeit.exception.ErrorCode;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BasicAuthService implements AuthService {

  private final UserRepository userRepository;

  @Override
  public User login(LoginRequest loginRequest) {
    String username = loginRequest.username();
    String password = loginRequest.password();

    // 유저 검색
    User user = userRepository.findByUsername(username)
        .orElseThrow(
            () -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 비밀번호 확인
    if (!user.getPassword().equals(password)) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    return user;
  }
}

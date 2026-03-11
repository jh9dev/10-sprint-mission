package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

  @Override
  @EntityGraph(attributePaths = {"profile", "userStatus"})
  Optional<User> findById(UUID userId);

  @EntityGraph(attributePaths = {"profile", "userStatus"})
  Optional<User> findByUsername(String username);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  @EntityGraph(attributePaths = {"profile", "userStatus"})
  @Query("SELECT u FROM User u")
  List<User> findAllWithProfileAndUserStatus();
}
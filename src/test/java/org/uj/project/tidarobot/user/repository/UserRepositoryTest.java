package org.uj.project.tidarobot.user.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.uj.project.tidarobot.config.JpaTestConfig;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JpaTestConfig.class)
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class UserRepositoryTest {

    @Autowired UserRepository userRepository;

    // --- existsByUsername / existsByEmail / findByUsername ---

    @Test
    void existsByUsername_existingUser_returnsTrue() {
        userRepository.save(user("alice", "alice@test.com", Role.USER, Status.APPROVED));

        assertThat(userRepository.existsByUsername("alice")).isTrue();
    }

    @Test
    void existsByUsername_unknownUsername_returnsFalse() {
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(user("alice", "alice@test.com", Role.USER, Status.APPROVED));

        assertThat(userRepository.existsByEmail("alice@test.com")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        userRepository.save(user("alice", "alice@test.com", Role.USER, Status.APPROVED));

        Optional<User> result = userRepository.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findByUsername_unknownUser_returnsEmpty() {
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    // --- UserSpecification ---

    @Test
    void findAll_hasRoleSpec_returnsOnlyMatchingRole() {
        userRepository.save(user("alice", "alice@test.com", Role.USER,  Status.APPROVED));
        userRepository.save(user("bob",   "bob@test.com",   Role.ADMIN, Status.APPROVED));

        Page<User> result = userRepository.findAll(UserSpecification.hasRole(Role.USER), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void findAll_hasStatusSpec_returnsOnlyMatchingStatus() {
        userRepository.save(user("alice", "alice@test.com", Role.USER, Status.APPROVED));
        userRepository.save(user("bob",   "bob@test.com",   Role.USER, Status.PENDING));

        Page<User> result = userRepository.findAll(UserSpecification.hasStatus(Status.PENDING), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("bob");
    }

    @Test
    void findAll_usernameContainsSpec_matchesCaseInsensitivePartial() {
        userRepository.save(user("alice",   "alice@test.com",   Role.USER, Status.APPROVED));
        userRepository.save(user("charlie", "charlie@test.com", Role.USER, Status.APPROVED));
        userRepository.save(user("bob",     "bob@test.com",     Role.USER, Status.APPROVED));

        // "LI" matches "alice" and "charlie" (case-insensitive contains)
        Page<User> result = userRepository.findAll(
                UserSpecification.usernameContains("LI"), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(User::getUsername)
                .containsExactlyInAnyOrder("alice", "charlie");
    }

    @Test
    void findAll_combinedRoleAndStatusSpec_appliesAndLogic() {
        userRepository.save(user("alice", "alice@test.com", Role.USER,  Status.APPROVED));
        userRepository.save(user("bob",   "bob@test.com",   Role.USER,  Status.PENDING));
        userRepository.save(user("carol", "carol@test.com", Role.ADMIN, Status.APPROVED));

        Specification<User> spec = Specification
                .where(UserSpecification.hasRole(Role.USER))
                .and(UserSpecification.hasStatus(Status.APPROVED));

        Page<User> result = userRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void findAll_nullFilters_returnsAllUsers() {
        userRepository.save(user("alice", "alice@test.com", Role.USER,  Status.APPROVED));
        userRepository.save(user("bob",   "bob@test.com",   Role.ADMIN, Status.PENDING));

        Specification<User> spec = Specification
                .where(UserSpecification.hasRole(null))
                .and(UserSpecification.hasStatus(null))
                .and(UserSpecification.usernameContains(null));

        Page<User> result = userRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    // --- helper ---

    private User user(String username, String email, Role role, Status status) {
        return User.builder()
                .username(username)
                .email(email)
                .passwordHash("hash")
                .role(role)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}

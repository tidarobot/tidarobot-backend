package org.uj.project.tidarobot.user.repository;

import org.springframework.data.jpa.domain.Specification;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;

public class UserSpecification {

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) ->
                role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasStatus(Status status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> usernameContains(String username) {
        return (root, query, cb) ->
                (username == null || username.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }
}

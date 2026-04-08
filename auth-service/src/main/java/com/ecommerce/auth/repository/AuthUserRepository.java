package com.ecommerce.auth.repository;

import com.ecommerce.auth.entity.AuthUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByUsername(String username);

    Optional<AuthUser> findByEmail(String email);
}

package com.hoang.crypto.repository;

import com.hoang.crypto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

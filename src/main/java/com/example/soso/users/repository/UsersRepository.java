package com.example.soso.users.repository;

import com.example.soso.users.domain.entity.Users;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsersRepository extends JpaRepository<Users, String> {


    @Query("SELECT u.nickname FROM Users as u")
    Set<String> findAllNicknames();

    boolean existsByEmail(String email);

    boolean existsById(String id);
}

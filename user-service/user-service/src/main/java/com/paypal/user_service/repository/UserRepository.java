package com.paypal.user_service.repository;

import com.paypal.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    //This method will be used in email login
    Optional<User>findByEmail(String email);
}

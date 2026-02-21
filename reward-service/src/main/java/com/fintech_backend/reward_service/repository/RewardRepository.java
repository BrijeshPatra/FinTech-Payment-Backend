package com.fintech_backend.reward_service.repository;

import com.fintech_backend.reward_service.entity.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RewardRepository extends JpaRepository<Reward,Long> {
    Optional<Reward>findByTransactionId(Long transactionId);

    boolean existsByTransactionId(Long transactionId);
}

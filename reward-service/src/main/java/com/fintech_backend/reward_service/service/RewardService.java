package com.fintech_backend.reward_service.service;

import com.fintech_backend.reward_service.dto.TransactionEvent;
import com.fintech_backend.reward_service.entity.Reward;

import java.util.List;

public interface RewardService {

    void processReward(TransactionEvent event);

    Reward sendReward(Reward reward);

    List<Reward>getRewardsById(Long userId);

}

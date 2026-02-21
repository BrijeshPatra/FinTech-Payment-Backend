package com.fintech_backend.reward_service.controller;

import com.fintech_backend.reward_service.entity.Reward;
import com.fintech_backend.reward_service.repository.RewardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reward")
public class RewardController {

    private final RewardRepository repository;

    public RewardController(RewardRepository repository){
        this.repository=repository;
    }

    @GetMapping
    public List<Reward>getAllRewards(){
        return repository.findAll();
    }

    @GetMapping("user/{userId}")
    public List<Reward>getRewardByUserId(@PathVariable Long userId){
        return repository.findByUserId(userId);
    }
}

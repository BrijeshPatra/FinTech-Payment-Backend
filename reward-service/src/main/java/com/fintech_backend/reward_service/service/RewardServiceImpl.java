package com.fintech_backend.reward_service.service;

import com.fintech_backend.reward_service.dto.TransactionEvent;
import com.fintech_backend.reward_service.entity.Reward;
import com.fintech_backend.reward_service.repository.RewardRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RewardServiceImpl implements RewardService{

    private final RewardRepository repository;
    private static final double REWARD_PERCENTAGE=0.01;

    private static final Logger log= LoggerFactory.getLogger(RewardServiceImpl.class);

    public RewardServiceImpl(RewardRepository repository){
        this.repository=repository;
    }

    @Override
    @Transactional
    public void processReward(TransactionEvent event) {
        log.info("Processing reward for transactionId={}",event.getTransactionId());

        if(!"SUCCESS".equalsIgnoreCase(event.getStatus())){
            log.info("Transaction {} is not successful. Skipping reward,",event.getTransactionId());
            return;
        }
        //Idempotency check if the ID already exists in DB
        if (repository.existsByTransactionId(event.getTransactionId())){
            log.warn("Reward exists for transaction id {}",event.getTransactionId());
        }

        double calcReward=event.getAmount()*REWARD_PERCENTAGE;
        Reward reward=new Reward();
        reward.setTransactionId(event.getTransactionId());
        reward.setUserId(event.getSenderId());
        reward.setPoints(calcReward);

        repository.save(reward);

        log.info("Reward created successfully for transaction id = {} with points={}",event.getTransactionId(),calcReward);
    }

    //send the reward

    @Override
    public Reward sendReward(Reward reward) {
        reward.setDeleveredAt(LocalDateTime.now());
        return repository.save(reward);
    }

    @Override
    public List<Reward> getRewardsById(Long userId) {
        return repository.findByUserId(userId);
    }
}

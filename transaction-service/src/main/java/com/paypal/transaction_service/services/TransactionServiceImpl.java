package com.paypal.transaction_service.services;

import com.paypal.transaction_service.dto.TransactionStatus;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.kafka.KafkaEventProducer;
import com.paypal.transaction_service.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaEventProducer kafkaEventProducer;
    private final ObjectMapper objectMapper;
    private static final Logger log= LoggerFactory.getLogger(TransactionServiceImpl.class);

    public TransactionServiceImpl(TransactionRepository transactionRepository,ObjectMapper objectMapper,
    KafkaEventProducer kafkaEventProducer ){
        this.transactionRepository=transactionRepository;
        this.objectMapper=objectMapper;
        this.kafkaEventProducer=kafkaEventProducer;
    }
    @Override
    @Transactional // it ensures DB operations are handled correctly
    public Transaction createTransaction(Transaction request) {

        Transaction transaction = new Transaction();
        transaction.setSenderId(request.getSenderId());
        transaction.setReceiverId(request.getReceiverId());
        transaction.setAmount(request.getAmount());
        transaction.setTimeStamp(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.SUCCESS);

       Transaction savedTransaction= transactionRepository.save(transaction);

       try{
           /*
           converts the Transaction ID from integer to string to serve as
           the kafka message key which ensures all updates for this specific
           transaction is being sent to the same partition
            */
           String key=String.valueOf(savedTransaction.getId());
           kafkaEventProducer.sendTransactionEvent(key,savedTransaction);
       }catch (Exception e){
          log.error("Failed to publish Kafka Event for transactionId: {}"
                  ,savedTransaction.getId(),e);
       }
       return savedTransaction;
    }


    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

}

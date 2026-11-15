package com.cap34.backend.service;

import com.cap34.backend.dto.ConsumerRequest;
import com.cap34.backend.entity.Consumer;
import com.cap34.backend.repository.ConsumerRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ConsumerService {
    private final ConsumerRepository repo;

    public ConsumerService(ConsumerRepository r) {
        repo = r;
    }

    public Consumer create(ConsumerRequest r) {
        Consumer c = new Consumer();
        c.setConsumerName(r.consumerName());
        c.setConsumerType(r.consumerType());
        c.setOwner(r.owner());
        return repo.save(c);
    }

    public List<Consumer> all() {
        return repo.findAll();
    }

    public Consumer get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Consumer not found"));
    }
}

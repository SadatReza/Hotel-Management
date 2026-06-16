package com.aurelia.grand.repository;

import com.aurelia.grand.model.CleaningRequest;
import com.aurelia.grand.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CleaningRequestRepository extends JpaRepository<CleaningRequest, Long> {
    List<CleaningRequest> findAllByOrderByRequestedAtDesc();
    List<CleaningRequest> findByRequestedByOrderByRequestedAtDesc(User user);
}

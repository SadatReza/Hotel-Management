package com.aurelia.grand.repository;

import com.aurelia.grand.model.ServiceOrder;
import com.aurelia.grand.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, Long> {
    List<ServiceOrder> findAllByOrderByOrderedAtDesc();
    List<ServiceOrder> findByUserOrderByOrderedAtDesc(User user);
    List<ServiceOrder> findByStatusIn(List<String> statuses);
}

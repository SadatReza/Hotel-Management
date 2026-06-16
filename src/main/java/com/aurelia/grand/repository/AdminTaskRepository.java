package com.aurelia.grand.repository;

import com.aurelia.grand.model.AdminTask;
import com.aurelia.grand.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminTaskRepository extends JpaRepository<AdminTask, Long> {
    List<AdminTask> findAllByOrderByCreatedAtDesc();
    List<AdminTask> findByAssignedToOrderByCreatedAtDesc(User user);
}

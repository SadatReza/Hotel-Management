package com.aurelia.grand.repository;

import com.aurelia.grand.model.StaffProfile;
import com.aurelia.grand.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByUser(User user);
    Optional<StaffProfile> findByUserId(Long userId);
}

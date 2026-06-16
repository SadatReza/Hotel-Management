package com.aurelia.grand.repository;

import com.aurelia.grand.model.Invoice;
import com.aurelia.grand.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByIssuedAtDesc();
    List<Invoice> findByUserOrderByIssuedAtDesc(User user);
}

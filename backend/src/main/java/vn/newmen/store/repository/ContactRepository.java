package vn.newmen.store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.Contact;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    Page<Contact> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

package vn.newmen.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.newmen.store.domain.InventoryMovement;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> { }

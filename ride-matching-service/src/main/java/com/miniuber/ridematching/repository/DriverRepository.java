package com.miniuber.ridematching.repository;

import com.miniuber.ridematching.model.Driver;
import com.miniuber.ridematching.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, String> {
    List<Driver> findByAvailableTrue();
    List<Driver> findByStatus(DriverStatus status);
}

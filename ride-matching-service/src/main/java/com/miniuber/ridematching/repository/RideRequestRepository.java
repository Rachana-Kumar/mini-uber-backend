package com.miniuber.ridematching.repository;

import com.miniuber.ridematching.model.RideRequest;
import com.miniuber.ridematching.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RideRequestRepository extends JpaRepository<RideRequest, String> {
    List<RideRequest> findByPassengerId(String passengerId);
    List<RideRequest> findByStatus(RideStatus status);
}

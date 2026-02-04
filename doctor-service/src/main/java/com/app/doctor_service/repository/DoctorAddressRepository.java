package com.app.doctor_service.repository;

import com.app.doctor_service.entity.DoctorAddress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAddressRepository extends JpaRepository<DoctorAddress, Long> {
}

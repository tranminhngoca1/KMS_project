package com.kms301.nkms.repository;

import com.kms301.nkms.model.CapTheDoiLap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CapTheDoiLapRepository extends JpaRepository<CapTheDoiLap, Long> {
}


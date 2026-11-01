package com.kms301.nkms.repository;

import com.kms301.nkms.domain.Nguon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NguonRepository extends JpaRepository<Nguon, Long> {
    Optional<Nguon> findByMa(String ma);
}


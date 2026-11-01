package com.kms301.nkms.repository;

import com.kms301.nkms.model.BangChuCai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BangChuCaiRepository extends JpaRepository<BangChuCai, Long> {
    Optional<BangChuCai> findByMa(String ma);
}


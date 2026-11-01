package com.kms301.nkms.repository;

import com.kms301.nkms.domain.BangChuCaiGiaTri;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BangChuCaiGiaTriRepository extends JpaRepository<BangChuCaiGiaTri, Long> {
    List<BangChuCaiGiaTri> findByBangChuCaiIdOrderByKyTuAsc(Long bangChuCaiId);
    Optional<BangChuCaiGiaTri> findByBangChuCaiIdAndKyTu(Long bangChuCaiId, String kyTu);
    void deleteByBangChuCaiId(Long bangChuCaiId);
}

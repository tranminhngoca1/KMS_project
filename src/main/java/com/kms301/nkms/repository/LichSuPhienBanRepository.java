package com.kms301.nkms.repository;

import com.kms301.nkms.model.LichSuPhienBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuPhienBanRepository extends JpaRepository<LichSuPhienBan, Long> {
    List<LichSuPhienBan> findByDonViIdOrderByPhienBanDescIdDesc(Long donViId);
}

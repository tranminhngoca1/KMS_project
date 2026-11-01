package com.kms301.nkms.repository;

import com.kms301.nkms.domain.DongNghia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DongNghiaRepository extends JpaRepository<DongNghia, Long> {
    List<DongNghia> findByKhaiNiemIdOrderByIdAsc(Long khaiNiemId);
    List<DongNghia> findAllByOrderByKhaiNiemIdAscIdAsc();
}


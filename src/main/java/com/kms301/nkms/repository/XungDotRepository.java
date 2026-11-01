package com.kms301.nkms.repository;

import com.kms301.nkms.domain.XungDot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface XungDotRepository extends JpaRepository<XungDot, Long> {
    Optional<XungDot> findByMa(String ma);

    Page<XungDot> findAllByOrderByIdAsc(Pageable pageable);

    @Query("SELECT x FROM XungDot x JOIN x.danhSachDonVi d WHERE d.id = :donViId ORDER BY x.id ASC")
    List<XungDot> findByDonViId(@Param("donViId") Long donViId);

    @Query("SELECT DISTINCT x FROM XungDot x LEFT JOIN FETCH x.khaiNiem LEFT JOIN FETCH x.danhSachDonVi d LEFT JOIN FETCH d.truongPhai LEFT JOIN FETCH d.nguon ORDER BY x.id ASC")
    List<XungDot> findAllWithUnits();

    long countByTrangThai(String trangThai);
}


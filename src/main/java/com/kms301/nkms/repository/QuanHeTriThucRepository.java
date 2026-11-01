package com.kms301.nkms.repository;

import com.kms301.nkms.domain.QuanHeTriThuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuanHeTriThucRepository extends JpaRepository<QuanHeTriThuc, Long> {
    @Query("SELECT q FROM QuanHeTriThuc q JOIN FETCH q.tuDonVi JOIN FETCH q.denDonVi WHERE q.tuDonVi.id = :donViId OR q.denDonVi.id = :donViId ORDER BY q.id ASC")
    List<QuanHeTriThuc> findAllRelatedToDonVi(@Param("donViId") Long donViId);

    @Query("SELECT q FROM QuanHeTriThuc q JOIN FETCH q.tuDonVi JOIN FETCH q.denDonVi ORDER BY q.id ASC")
    List<QuanHeTriThuc> findAllWithUnits();
}


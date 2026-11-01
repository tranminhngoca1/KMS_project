package com.kms301.nkms.repository;

import com.kms301.nkms.domain.KhaiNiem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhaiNiemRepository extends JpaRepository<KhaiNiem, Long> {
    Optional<KhaiNiem> findByMa(String ma);
    List<KhaiNiem> findAllByOrderByIdAsc();
}


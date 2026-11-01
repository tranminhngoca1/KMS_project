package com.kms301.nkms.repository;

import com.kms301.nkms.model.TruongPhai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TruongPhaiRepository extends JpaRepository<TruongPhai, Long> {
    Optional<TruongPhai> findByMa(String ma);
}


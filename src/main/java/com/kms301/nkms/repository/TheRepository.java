package com.kms301.nkms.repository;

import com.kms301.nkms.model.The;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheRepository extends JpaRepository<The, Long> {
    Optional<The> findByMa(String ma);
    List<The> findAllByOrderByNhomAscTenAsc();
}


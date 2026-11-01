package com.kms301.nkms.repository;

import com.kms301.nkms.model.DonViTriThuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonViTriThucRepository extends JpaRepository<DonViTriThuc, Long>, JpaSpecificationExecutor<DonViTriThuc> {
    Optional<DonViTriThuc> findByMa(String ma);

    @Query("SELECT DISTINCT d FROM DonViTriThuc d " +
            "LEFT JOIN FETCH d.khaiNiem " +
            "LEFT JOIN FETCH d.truongPhai " +
            "LEFT JOIN FETCH d.nguon n " +
            "LEFT JOIN FETCH n.nguonGoc " +
            "LEFT JOIN FETCH d.danhSachThe " +
            "WHERE d.trangThai = :trangThai AND d.nhanSuDung IN :nhanSuDungList " +
            "ORDER BY d.id ASC")
    List<DonViTriThuc> findByTrangThaiAndNhanSuDungInOrderByIdAsc(
            @Param("trangThai") String trangThai,
            @Param("nhanSuDungList") Collection<String> nhanSuDungList);

    List<DonViTriThuc> findByKhaiNiemIdAndGiaTriKichHoatAndIdNot(Long khaiNiemId, String giaTriKichHoat, Long excludeId);

    long countByNguonId(Long nguonId);
    long countByTruongPhaiId(Long truongPhaiId);
    long countByNhanSuDung(String nhanSuDung);
    long countByMucTinCay(String mucTinCay);
    long countByTrangThai(String trangThai);

    @Query("SELECT COUNT(d) FROM DonViTriThuc d WHERE d.trangTu IS NOT NULL")
    long countHasPageNumber();

    @Query("SELECT COUNT(d) FROM DonViTriThuc d WHERE d.trangTu IS NULL")
    long countMissingPageNumber();
}


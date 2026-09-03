package com.leisure.region.repository;

import com.leisure.region.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByLdongRegnCdAndLdongSignguCd(String ldongRegnCd, String ldongSignguCd);
}

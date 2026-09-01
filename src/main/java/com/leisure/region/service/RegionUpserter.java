package com.leisure.region.service;

import com.leisure.region.domain.Region;
import com.leisure.region.dto.RegionData;
import com.leisure.region.dto.result.RegionSyncResult;
import com.leisure.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RegionUpserter {

    private static final Logger log = LoggerFactory.getLogger(RegionUpserter.class);

    private final RegionRepository repository;

    @Transactional
    public RegionSyncResult upsert(List<RegionData> list) {

        int inserted = 0, updated = 0;

        for (RegionData regionData : list) {
            Optional<Region> region = repository.findByLdongRegnCdAndLdongSignguCd(regionData.ldongRegnCd(), regionData.ldongSignguCd());

            if (region.isPresent()) {
                region.get().updateNames(regionData.regnName(), regionData.signguName());
                updated++;
            } else {
                repository.save(Region.create(regionData.ldongRegnCd(), regionData.ldongSignguCd(), regionData.regnName(), regionData.signguName()));
                inserted++;
            }
        }

        log.info("[region-sync] 완료 inserted={}, updated={}", inserted, updated);

        return new RegionSyncResult(inserted, updated, (inserted + updated));
    }
}

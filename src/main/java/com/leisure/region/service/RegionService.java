package com.leisure.region.service;

import com.leisure.global.external.tourapi.TourApiClient;
import com.leisure.global.external.tourapi.dto.response.LdongCodeResponse.Item;
import com.leisure.region.dto.command.RegionData;
import com.leisure.region.dto.result.RegionSyncResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private static final Logger log = LoggerFactory.getLogger(RegionService.class);

    private final TourApiClient client;

    private final RegionWriter writer;

    public void syncRegions() {

        List<RegionData> list = new ArrayList<>();

        List<Item> areas = client.fetchRegions().response().body().items().item();

        for (Item area : areas) {
            List<Item> sigungus = client.fetchSigungus(area.code()).response().body().items().item();

            for (Item sigungu : sigungus) {
                list.add(new RegionData(area.code(), sigungu.code(), area.name(), sigungu.name()));
            }

        }

        RegionSyncResult result = writer.updates(list);

        log.info("[region-sync] 완료 inserted={} updated={} total={}",
                result.inserted(), result.updated(), result.total());
    }
}

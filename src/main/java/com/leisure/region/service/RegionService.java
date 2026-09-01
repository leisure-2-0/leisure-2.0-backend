package com.leisure.region.service;

import com.leisure.global.external.tourapi.TourApiClient;
import com.leisure.global.external.tourapi.dto.response.LdongCodeResponse.Item;
import com.leisure.region.dto.RegionData;
import com.leisure.region.dto.response.RegionSyncResponse;
import com.leisure.region.dto.result.RegionSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final TourApiClient client;

    private final RegionUpserter upserter;

    public RegionSyncResponse syncRegions() {

        List<RegionData> list = new ArrayList<>();

        List<Item> areas = client.fetchRegions().response().body().items().item();

        for (Item area : areas) {
            List<Item> sigungus = client.fetchSigungus(area.code()).response().body().items().item();

            for (Item sigungu : sigungus) {
                list.add(new RegionData(area.code(), sigungu.code(), area.name(), sigungu.name()));
            }

        }

        RegionSyncResult result = upserter.upsert(list);
        return new RegionSyncResponse(result.inserted(), result.updated(), result.total());
    }
}

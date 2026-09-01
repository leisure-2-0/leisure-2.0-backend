package com.leisure.region.controller;

import com.leisure.global.response.ApiResponse;
import com.leisure.region.dto.response.RegionSyncResponse;
import com.leisure.region.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegionController {

    private final RegionService service;

    @PostMapping("/regions/sync")
    public ResponseEntity<ApiResponse<RegionSyncResponse>> syncRegions() {

        RegionSyncResponse response = service.syncRegions();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("지역 정보를 갱신했습니다.", response));
    }
}

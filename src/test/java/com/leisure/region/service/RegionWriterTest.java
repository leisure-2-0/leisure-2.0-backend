package com.leisure.region.service;

import com.leisure.region.domain.Region;
import com.leisure.region.dto.command.RegionData;
import com.leisure.region.dto.result.RegionSyncResult;
import com.leisure.region.repository.RegionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("지역 업서트 (RegionWriter.updates)")
class RegionWriterTest {

    @Mock
    private RegionRepository repository;

    @InjectMocks
    private RegionWriter writer;

    @Test
    @DisplayName("기존 지역이 없으면 저장하고 inserted를 센다")
    void insert() {
        given(repository.findByLdongRegnCdAndLdongSignguCd("51", "150")).willReturn(Optional.empty());

        RegionSyncResult result = writer.updates(List.of(new RegionData("51", "150", "강원특별자치도", "강릉시")));

        verify(repository).save(any(Region.class));
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("기존 지역이 있으면 이름만 갱신하고 저장하지 않는다(updated)")
    void update() {
        Region existing = Region.create("51", "150", "강원도", "강릉");
        given(repository.findByLdongRegnCdAndLdongSignguCd("51", "150")).willReturn(Optional.of(existing));

        RegionSyncResult result = writer.updates(List.of(new RegionData("51", "150", "강원특별자치도", "강릉시")));

        verify(repository, never()).save(any());
        assertThat(existing.getRegnName()).isEqualTo("강원특별자치도");
        assertThat(existing.getSignguName()).isEqualTo("강릉시");
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.inserted()).isZero();
    }

    @Test
    @DisplayName("신규와 기존이 섞이면 각각 집계한다")
    void mixed() {
        given(repository.findByLdongRegnCdAndLdongSignguCd("51", "150")).willReturn(Optional.empty());
        given(repository.findByLdongRegnCdAndLdongSignguCd("51", "210"))
                .willReturn(Optional.of(Region.create("51", "210", "강원", "원주")));

        RegionSyncResult result = writer.updates(List.of(
                new RegionData("51", "150", "강원특별자치도", "강릉시"),
                new RegionData("51", "210", "강원특별자치도", "원주시")));

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2);
    }
}

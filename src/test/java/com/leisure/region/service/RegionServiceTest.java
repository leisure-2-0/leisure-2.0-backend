package com.leisure.region.service;

import com.leisure.global.external.tourapi.TourApiClient;
import com.leisure.global.external.tourapi.dto.response.LdongCodeResponse;
import com.leisure.region.dto.command.RegionData;
import com.leisure.region.dto.result.RegionSyncResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("지역 동기화 (RegionService.syncRegions)")
class RegionServiceTest {

    @Mock
    private TourApiClient client;

    @Mock
    private RegionWriter writer;

    @InjectMocks
    private RegionService service;

    @Captor
    private ArgumentCaptor<List<RegionData>> dataCaptor;

    private LdongCodeResponse response(LdongCodeResponse.Item... items) {
        return new LdongCodeResponse(new LdongCodeResponse.Response(
                new LdongCodeResponse.Header("0000", "OK"),
                new LdongCodeResponse.Body(new LdongCodeResponse.Items(List.of(items)))));
    }

    @Test
    @DisplayName("광역×시군구를 조합해 RegionData 목록을 만들어 writer에 넘긴다")
    void buildsCartesian() {
        given(client.fetchRegions()).willReturn(response(new LdongCodeResponse.Item("51", "강원특별자치도")));
        given(client.fetchSigungus("51")).willReturn(response(
                new LdongCodeResponse.Item("150", "강릉시"),
                new LdongCodeResponse.Item("210", "원주시")));
        given(writer.updates(dataCaptor.capture())).willReturn(new RegionSyncResult(2, 0, 2));

        service.syncRegions();

        List<RegionData> passed = dataCaptor.getValue();
        assertThat(passed).containsExactly(
                new RegionData("51", "150", "강원특별자치도", "강릉시"),
                new RegionData("51", "210", "강원특별자치도", "원주시"));
    }
}

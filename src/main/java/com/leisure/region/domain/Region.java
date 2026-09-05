package com.leisure.region.domain;

import com.leisure.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_regions_area_sigungu",
                columnNames = {"ldong_regn_cd", "ldong_signgu_cd"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Region extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "ldong_regn_cd", nullable = false, comment = "법정동 광역 코드 (예: 51 강원)")
    private String ldongRegnCd;

    @Column(name = "ldong_signgu_cd", nullable = false, comment = "법정동 시군구 코드(예: 150 강릉)")
    private String ldongSignguCd;

    @Column(name = "regn_name", nullable = false, comment = "광역명 (예: 강원특별자치도)")
    private String regnName;

    @Column(name = "signgu_name", nullable = false, comment = "시군구명 (예: 강릉시)")
    private String signguName;

    public void updateNames(String regnName, String signguName) {
        this.regnName = regnName;
        this.signguName = signguName;
    }

    private Region(String ldongRegnCd, String ldongSignguCd, String regnName, String signguName) {
        this.ldongRegnCd = ldongRegnCd;
        this.ldongSignguCd = ldongSignguCd;
        this.regnName = regnName;
        this.signguName = signguName;
    }

    public static Region create(String ldongRegnCd, String ldongSignguCd, String regnName, String signguName) {
        return new Region(ldongRegnCd, ldongSignguCd, regnName, signguName);
    }
}

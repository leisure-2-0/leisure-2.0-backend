package com.leisure.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
// @SQLRestriction("deleted_at is null")
public class BaseSoftDeleteEntity extends BaseTimeEntity{

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

//    public boolean isDeleted() {
//        return this.deletedAt != null;
//    }
}


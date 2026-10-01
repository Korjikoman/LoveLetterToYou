package com.example.myproject.Images.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image_quota_lock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageQuotaLock {
    @Id
    private Short id;
}
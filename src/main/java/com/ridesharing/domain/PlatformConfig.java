package com.ridesharing.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfig {

    @Id
    @Column(name = "\"key\"")
    private String key;

    @Column(name = "config_value", nullable = false)
    private String value;

    @Column
    private LocalDateTime updatedAt;
}

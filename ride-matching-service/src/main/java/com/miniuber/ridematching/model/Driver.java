package com.miniuber.ridematching.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String phone;
    private boolean available;

    @Enumerated(EnumType.STRING)
    private DriverStatus status;
}

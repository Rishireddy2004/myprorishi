package com.ridesharing.dto;

import jakarta.validation.constraints.NotNull;

public class VehicleDTO {

    @NotNull
    private String make;

    @NotNull
    private String model;

    @NotNull
    private Integer year;

    @NotNull
    private String color;

    @NotNull
    private String licensePlate;

    @NotNull
    private Integer passengerCapacity;

    public VehicleDTO() {}

    public VehicleDTO(String make, String model, Integer year, String color,
                      String licensePlate, Integer passengerCapacity) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.licensePlate = licensePlate;
        this.passengerCapacity = passengerCapacity;
    }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public Integer getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(Integer passengerCapacity) { this.passengerCapacity = passengerCapacity; }
}

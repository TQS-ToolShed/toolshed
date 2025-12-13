package com.toolshed.backend.dto;

import java.util.UUID;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 DTO used by the PUT endpoint. Fields are nullable so the controller can perform
 partial updates: only non-null fields from the request will be applied.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateToolInput {
    private String title;

    @Size(max = 1000)
    private String description;

    private Double pricePerDay;

    private String district;

    private UUID ownerId;

    private Boolean active;

    private String availabilityCalendar;

    private Double overallRating;

    public Integer getNumRatings() {
        return numRatings;
    }

    public void setNumRatings(Integer numRatings) {
        this.numRatings = numRatings;
    }

    public Double getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(Double overallRating) {
        this.overallRating = overallRating;
    }

    public String getAvailabilityCalendar() {
        return availabilityCalendar;
    }

    public void setAvailabilityCalendar(String availabilityCalendar) {
        this.availabilityCalendar = availabilityCalendar;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(Double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private Integer numRatings;
}

package com.example.facility.ticket.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotNull
    private Long deviceId;

    @NotNull
    private Long categoryId;

    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private Float severityScore;

    @NotBlank
    private String description;
}


package com.sahastra.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogNameRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
    @NotBlank
    @Size(max = 140)
    private String slug;
}

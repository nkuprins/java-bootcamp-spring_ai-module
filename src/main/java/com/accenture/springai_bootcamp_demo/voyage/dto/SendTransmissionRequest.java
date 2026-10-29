package com.accenture.springai_bootcamp_demo.voyage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendTransmissionRequest(
        @NotBlank(message = "message must not be blank")
        @Size(max = 500, message = "message must not exceed 500 characters")
        String message) {
}

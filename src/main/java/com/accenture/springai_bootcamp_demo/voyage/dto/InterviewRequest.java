package com.accenture.springai_bootcamp_demo.voyage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InterviewRequest(
        @NotBlank(message = "question must not be blank")
        @Size(max = 500, message = "question must not exceed 500 characters")
        String question) {
}

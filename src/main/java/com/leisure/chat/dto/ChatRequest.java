package com.leisure.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(

        @NotBlank String question,

        Long roomId
) {}

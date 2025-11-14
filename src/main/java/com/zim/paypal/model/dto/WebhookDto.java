package com.zim.paypal.model.dto;

import com.zim.paypal.model.entity.Webhook;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for webhook creation
 * 
 * @author dexterwura
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDto {

    @NotBlank(message = "URL is required")
    private String url;

    @Builder.Default
    private List<Webhook.EventType> events = new ArrayList<>();

    private String description;
}


package com.akkidarkhq.visaflow.dto.response;

import com.akkidarkhq.visaflow.model.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisaOrderResponse {
    private UUID id;
    private String orderReference;
    private UUID userId;
    private BigDecimal totalAmount;
    private String currency;
    private OrderStatus status;
    private List<OrderItemResponse> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

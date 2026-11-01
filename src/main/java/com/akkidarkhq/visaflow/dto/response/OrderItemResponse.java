package com.akkidarkhq.visaflow.dto.response;

import com.akkidarkhq.visaflow.model.enums.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private UUID id;
    private UUID travellerId;
    private String travellerFullName;
    private String passportNumber;
    private Long visaTypeId;
    private String visaTypeName;
    private BigDecimal serviceFee;
    private BigDecimal embassyFee;
    private BigDecimal totalAmount;
    private String currency;
    private OrderItemStatus status;
    private UUID visaApplicationId;
    private String visaApplicationReference;
}

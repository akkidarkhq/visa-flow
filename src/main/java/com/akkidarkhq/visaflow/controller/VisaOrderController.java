package com.akkidarkhq.visaflow.controller;

import com.akkidarkhq.visaflow.dto.ApiResponse;
import com.akkidarkhq.visaflow.dto.request.CheckoutOrderRequest;
import com.akkidarkhq.visaflow.dto.response.VisaOrderResponse;
import com.akkidarkhq.visaflow.service.VisaOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for Order checkouts, viewing user orders, and payment processing.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Visa Orders & Payments", description = "Endpoints for checkout, order lifecycle, and payment confirmation")
public class VisaOrderController {

    private final VisaOrderService visaOrderService;

    /**
     * Creates an order for verified applications (Model B) or new travellers (Model A).
     *
     * @param request Checkout request containing applicationIds or travellerIds + visaTypeId
     * @return Created VisaOrder in PENDING_PAYMENT status
     */
    @PostMapping("/checkout")
    @Operation(summary = "Checkout visa order",
            description = "Creates an order for payment. For Model B, provide 'applicationIds' of verified applications (DOCUMENTS_COMPLETE).")
    public ResponseEntity<ApiResponse<VisaOrderResponse>> checkout(
            @Valid @RequestBody CheckoutOrderRequest request) {
        VisaOrderResponse response = visaOrderService.checkout(request);
        return new ResponseEntity<>(
                ApiResponse.success("Order and applications created successfully", response),
                HttpStatus.CREATED
        );
    }

    /**
     * Retrieves an order by its unique ID.
     *
     * @param id Order UUID
     * @return Order details including line items and linked applications
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Fetches complete order details, items, fees, and application references.")
    public ResponseEntity<ApiResponse<VisaOrderResponse>> getOrderById(
            @Parameter(description = "Order UUID")
            @PathVariable UUID id) {
        VisaOrderResponse response = visaOrderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves all orders for a specific user ID.
     *
     * @param userId User UUID
     * @return List of user orders
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by User ID", description = "Retrieves all visa orders placed by a specific user.")
    public ResponseEntity<ApiResponse<List<VisaOrderResponse>>> getOrdersByUserId(
            @Parameter(description = "User UUID")
            @PathVariable UUID userId) {
        List<VisaOrderResponse> responses = visaOrderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Marks an order as PAID, records payment transaction, and advances applications to SUBMITTED.
     *
     * @param id Order UUID
     * @return Updated order response
     */
    @PostMapping("/{id}/pay")
    @Operation(summary = "Record payment for order",
            description = "Transitions order to PAID, order items to PROCESSING, records a Payment audit record, and advances linked applications to SUBMITTED.")
    public ResponseEntity<ApiResponse<VisaOrderResponse>> markOrderAsPaid(
            @Parameter(description = "Order UUID")
            @PathVariable UUID id) {
        VisaOrderResponse response = visaOrderService.markOrderAsPaid(id);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully", response));
    }
}

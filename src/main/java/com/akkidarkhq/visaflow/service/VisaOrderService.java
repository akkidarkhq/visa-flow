package com.akkidarkhq.visaflow.service;

import com.akkidarkhq.visaflow.dto.request.CheckoutOrderRequest;
import com.akkidarkhq.visaflow.dto.response.OrderItemResponse;
import com.akkidarkhq.visaflow.dto.response.VisaOrderResponse;
import com.akkidarkhq.visaflow.exception.ResourceNotFoundException;
import com.akkidarkhq.visaflow.model.*;
import com.akkidarkhq.visaflow.model.enums.*;
import com.akkidarkhq.visaflow.model.payment.Payment;
import com.akkidarkhq.visaflow.repository.*;
import com.akkidarkhq.visaflow.security.model.User;
import com.akkidarkhq.visaflow.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisaOrderService {

    private final VisaOrderRepository visaOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final TravellerProfileRepository travellerProfileRepository;
    private final VisaTypeRepository visaTypeRepository;
    private final VisaApplicationRepository visaApplicationRepository;
    private final PaymentRepository paymentRepository;
    private final VisaApplicationService visaApplicationService;

    @Transactional
    public VisaOrderResponse checkout(CheckoutOrderRequest request) {
        if (request.getApplicationIds() != null && !request.getApplicationIds().isEmpty()) {
            return checkoutApplications(request);
        } else if (request.getTravellerIds() != null && !request.getTravellerIds().isEmpty() && request.getVisaTypeId() != null) {
            return checkoutTravellersLegacy(request);
        } else {
            throw new IllegalArgumentException("Either applicationIds or (travellerIds and visaTypeId) must be provided for checkout.");
        }
    }

    /**
     * MODEL B Checkout (Document-First Flow):
     * Takes verified VisaApplications (DOCUMENTS_COMPLETE), links them to OrderItems,
     * and sets order to PENDING_PAYMENT.
     */
    private VisaOrderResponse checkoutApplications(CheckoutOrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        List<VisaApplication> applications = new ArrayList<>();
        BigDecimal orderTotal = BigDecimal.ZERO;
        String currency = "INR";

        for (UUID applicationId : request.getApplicationIds()) {
            VisaApplication app = visaApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("VisaApplication not found with id: " + applicationId));

            // Verify ownership: traveler belongs to the user
            if (app.getTraveller() != null && app.getTraveller().getUser() != null) {
                if (!app.getTraveller().getUser().getId().equals(user.getId())) {
                    throw new AccessDeniedException("VisaApplication " + app.getReferenceNumber() + " does not belong to the user.");
                }
            }

            // Verify document verification status
            if (app.getStatus() != ApplicationStatus.DOCUMENTS_COMPLETE) {
                throw new IllegalStateException(
                        "Application " + app.getReferenceNumber() + " cannot be checked out. " +
                        "All mandatory documents must be verified and approved first. Current status: " + app.getStatus()
                );
            }

            // Verify not already linked
            if (app.getOrderItem() != null) {
                throw new IllegalStateException("Application " + app.getReferenceNumber() + " is already linked to an order.");
            }

            applications.add(app);

            BigDecimal sFee = app.getVisaType().getServiceFee() != null ? app.getVisaType().getServiceFee() : BigDecimal.ZERO;
            BigDecimal eFee = app.getVisaType().getEmbassyFee() != null ? app.getVisaType().getEmbassyFee() : BigDecimal.ZERO;
            orderTotal = orderTotal.add(sFee.add(eFee));

            if (app.getVisaType().getCurrency() != null) {
                currency = app.getVisaType().getCurrency();
            }
        }

        String orderReference = generateOrderReference();

        VisaOrder order = VisaOrder.builder()
                .orderReference(orderReference)
                .user(user)
                .totalAmount(orderTotal)
                .currency(currency)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        VisaOrder savedOrder = visaOrderRepository.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (VisaApplication app : applications) {
            BigDecimal serviceFee = app.getVisaType().getServiceFee() != null ? app.getVisaType().getServiceFee() : BigDecimal.ZERO;
            BigDecimal embassyFee = app.getVisaType().getEmbassyFee() != null ? app.getVisaType().getEmbassyFee() : BigDecimal.ZERO;
            BigDecimal totalAmount = serviceFee.add(embassyFee);

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .traveller(app.getTraveller())
                    .visaType(app.getVisaType())
                    .serviceFee(serviceFee)
                    .embassyFee(embassyFee)
                    .totalAmount(totalAmount)
                    .currency(savedOrder.getCurrency())
                    .status(OrderItemStatus.PENDING)
                    .build();

            OrderItem savedItem = orderItemRepository.save(orderItem);

            // Link application to OrderItem
            visaApplicationService.linkToOrderItem(app.getId(), savedItem);

            itemResponses.add(mapToItemResponse(savedItem, app));
        }

        return mapToOrderResponse(savedOrder, itemResponses);
    }

    /**
     * MODEL A Legacy Checkout:
     * Creates an order with traveller IDs and initializes blank applications.
     */
    private VisaOrderResponse checkoutTravellersLegacy(CheckoutOrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        VisaType visaType = visaTypeRepository.findById(request.getVisaTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("VisaType not found with id: " + request.getVisaTypeId()));

        List<TravellerProfile> travellers = new ArrayList<>();
        for (UUID travellerId : request.getTravellerIds()) {
            TravellerProfile traveller = travellerProfileRepository.findById(travellerId)
                    .orElseThrow(() -> new ResourceNotFoundException("TravellerProfile not found with id: " + travellerId));
            travellers.add(traveller);
        }

        BigDecimal serviceFee = visaType.getServiceFee() != null ? visaType.getServiceFee() : BigDecimal.ZERO;
        BigDecimal embassyFee = visaType.getEmbassyFee() != null ? visaType.getEmbassyFee() : BigDecimal.ZERO;
        BigDecimal perPersonTotal = serviceFee.add(embassyFee);
        BigDecimal orderTotal = perPersonTotal.multiply(BigDecimal.valueOf(travellers.size()));

        String orderReference = generateOrderReference();

        VisaOrder order = VisaOrder.builder()
                .orderReference(orderReference)
                .user(user)
                .totalAmount(orderTotal)
                .currency(visaType.getCurrency() != null ? visaType.getCurrency() : "INR")
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        VisaOrder savedOrder = visaOrderRepository.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (TravellerProfile traveller : travellers) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .traveller(traveller)
                    .visaType(visaType)
                    .serviceFee(serviceFee)
                    .embassyFee(embassyFee)
                    .totalAmount(perPersonTotal)
                    .currency(savedOrder.getCurrency())
                    .status(OrderItemStatus.PENDING)
                    .build();

            OrderItem savedItem = orderItemRepository.save(orderItem);

            VisaApplication savedApplication = visaApplicationService.createApplicationForOrderItem(
                    savedItem,
                    traveller,
                    visaType,
                    "Application automatically created via order " + orderReference
            );
            itemResponses.add(mapToItemResponse(savedItem, savedApplication));
        }
        return mapToOrderResponse(savedOrder, itemResponses);
    }

    /**
     * Optimized getOrderById:
     * Uses JOIN FETCH on traveller and visaType and a single batch query for linked applications.
     */
    @Transactional(readOnly = true)
    public VisaOrderResponse getOrderById(UUID orderId) {
        VisaOrder order = visaOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaOrder not found with id: " + orderId));

        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);

        List<UUID> itemIds = items.stream().map(OrderItem::getId).toList();
        Map<UUID, VisaApplication> appMap = visaApplicationRepository.findByOrderItemIdIn(itemIds)
                .stream()
                .filter(a -> a.getOrderItem() != null)
                .collect(Collectors.toMap(a -> a.getOrderItem().getId(), a -> a, (a1, a2) -> a1));

        List<OrderItemResponse> itemResponses = items.stream().map(item -> {
            VisaApplication app = appMap.get(item.getId());
            return mapToItemResponse(item, app);
        }).toList();

        return mapToOrderResponse(order, itemResponses);
    }

    /**
     * Optimized getOrdersByUserId:
     * Uses batch JOIN FETCH across all user orders, avoiding N+1 queries entirely.
     */
    @Transactional(readOnly = true)
    public List<VisaOrderResponse> getOrdersByUserId(UUID userId) {
        List<VisaOrder> orders = visaOrderRepository.findByUserId(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        List<UUID> orderIds = orders.stream().map(VisaOrder::getId).toList();
        List<OrderItem> allItems = orderItemRepository.findByOrderIdInWithDetails(orderIds);

        List<UUID> itemIds = allItems.stream().map(OrderItem::getId).toList();
        Map<UUID, VisaApplication> appMap = visaApplicationRepository.findByOrderItemIdIn(itemIds)
                .stream()
                .filter(a -> a.getOrderItem() != null)
                .collect(Collectors.toMap(a -> a.getOrderItem().getId(), a -> a, (a1, a2) -> a1));

        Map<UUID, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream().map(order -> {
            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), List.of());
            List<OrderItemResponse> itemResponses = items.stream().map(item -> {
                VisaApplication app = appMap.get(item.getId());
                return mapToItemResponse(item, app);
            }).toList();
            return mapToOrderResponse(order, itemResponses);
        }).toList();
    }

    /**
     * Marks an order as PAID, transitions order items to PROCESSING,
     * advances linked VisaApplications to SUBMITTED, and persists a Payment record.
     */
    @Transactional
    public VisaOrderResponse markOrderAsPaid(UUID orderId) {
        VisaOrder order = visaOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("VisaOrder not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.PAID) {
            return getOrderById(orderId);
        }

        order.setStatus(OrderStatus.PAID);
        VisaOrder updatedOrder = visaOrderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderIdWithDetails(orderId);
        List<UUID> itemIds = items.stream().map(OrderItem::getId).toList();
        List<VisaApplication> linkedApps = visaApplicationRepository.findByOrderItemIdIn(itemIds);

        for (OrderItem item : items) {
            item.setStatus(OrderItemStatus.PROCESSING);
            orderItemRepository.save(item);
        }

        for (VisaApplication app : linkedApps) {
            ApplicationStatus previousStatus = app.getStatus();
            app.setStatus(ApplicationStatus.SUBMITTED);
            visaApplicationRepository.save(app);
            visaApplicationService.saveHistory(app, previousStatus, ApplicationStatus.SUBMITTED,
                    "Payment confirmed for order " + order.getOrderReference() + ". Application submitted to embassy queue.");
        }

        // Record payment in Payment entity
        Payment payment = Payment.builder()
                .order(updatedOrder)
                .user(order.getUser())
                .razorpayOrderId("ord_pay_" + order.getOrderReference())
                .razorpayPaymentId("pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(PaymentGatewayStatus.CAPTURED)
                .paymentMethod(PaymentMethod.UPI)
                .paidAt(OffsetDateTime.now())
                .build();
        paymentRepository.save(payment);

        return getOrderById(orderId);
    }

    /**
     * Collision-proof order reference generator.
     * Checks database existence before returning.
     */
    private String generateOrderReference() {
        String ref;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(100000, 999999);
            ref = "ORD-" + Year.now().getValue() + "-" + randomNum;
        } while (visaOrderRepository.existsByOrderReference(ref));
        return ref;
    }

    private OrderItemResponse mapToItemResponse(OrderItem item, VisaApplication application) {
        String travellerName = item.getTraveller() != null
                ? item.getTraveller().getFirstName() + " " + item.getTraveller().getLastName()
                : null;
        String passport = item.getTraveller() != null ? item.getTraveller().getPassportNumber() : null;
        String visaName = item.getVisaType() != null ? item.getVisaType().getName() : null;

        return OrderItemResponse.builder()
                .id(item.getId())
                .travellerId(item.getTraveller() != null ? item.getTraveller().getId() : null)
                .travellerFullName(travellerName)
                .passportNumber(passport)
                .visaTypeId(item.getVisaType() != null ? item.getVisaType().getId() : null)
                .visaTypeName(visaName)
                .serviceFee(item.getServiceFee())
                .embassyFee(item.getEmbassyFee())
                .totalAmount(item.getTotalAmount())
                .currency(item.getCurrency())
                .status(item.getStatus())
                .visaApplicationId(application != null ? application.getId() : null)
                .visaApplicationReference(application != null ? application.getReferenceNumber() : null)
                .build();
    }

    private VisaOrderResponse mapToOrderResponse(VisaOrder order, List<OrderItemResponse> items) {
        return VisaOrderResponse.builder()
                .id(order.getId())
                .orderReference(order.getOrderReference())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

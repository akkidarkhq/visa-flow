package com.akkidarkhq.visaflow.repository;

import com.akkidarkhq.visaflow.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Optimized fetch join: retrieves order items with their traveller and visa type
     * in a single SQL query, completely eliminating N+1 lazy loading queries.
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN FETCH oi.traveller " +
           "JOIN FETCH oi.visaType " +
           "WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithDetails(@Param("orderId") UUID orderId);

    /**
     * Batch fetch join: retrieves all order items for multiple orders in a single SQL query.
     */
    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN FETCH oi.traveller " +
           "JOIN FETCH oi.visaType " +
           "WHERE oi.order.id IN :orderIds")
    List<OrderItem> findByOrderIdInWithDetails(@Param("orderIds") List<UUID> orderIds);
}

package com.ecommerce.project.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.project.model.Order;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    Double getTotalRevenue();

    // ── Reconciliation ────────────────────────────────────────────────────

    /**
     * Fetch all orders whose status is currently PENDING.
     * Used by the reconciliation scheduler to bulk-promote to PROCESSING.
     */
    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'PENDING'")
    List<Order> findAllPendingOrders();

    /**
     * Bulk-update in a single SQL statement instead of loading every entity.
     * This is the most efficient path when there could be many PENDING orders.
     */
    @Modifying
    @Query("UPDATE Order o SET o.orderStatus = 'PROCESSING' WHERE o.orderStatus = 'PENDING'")
    int bulkPromotePendingToProcessing();
}
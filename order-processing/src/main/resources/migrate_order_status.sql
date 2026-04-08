-- ============================================================
-- Migration: align existing order_status values with OrderStatus enum
-- Run this ONCE before deploying the updated application.
-- ============================================================

-- The old code used plain Strings like "Accepted", "Rejected", etc.
-- The new enum uses: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
-- Map old values to the closest new enum constant:

UPDATE orders SET order_status = 'PENDING'     WHERE order_status IN ('Pending', 'pending', 'Accepted', 'accepted');
UPDATE orders SET order_status = 'PROCESSING'  WHERE order_status IN ('Processing', 'processing', 'Confirmed', 'confirmed');
UPDATE orders SET order_status = 'SHIPPED'     WHERE order_status IN ('Shipped', 'shipped', 'Dispatched', 'dispatched');
UPDATE orders SET order_status = 'DELIVERED'   WHERE order_status IN ('Delivered', 'delivered', 'Completed', 'completed');
UPDATE orders SET order_status = 'CANCELLED'   WHERE order_status IN ('Cancelled', 'cancelled', 'Rejected', 'rejected', 'Canceled', 'canceled');

-- Catch-all: any remaining unknown values → PENDING
UPDATE orders SET order_status = 'PENDING'
WHERE order_status NOT IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED');

-- Verify: this should return 0 rows after migration
SELECT order_status, COUNT(*) FROM orders
WHERE order_status NOT IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')
GROUP BY order_status;

package servlet.dao;

import servlet.model.Payment;

import java.sql.*;

/**
 * PaymentDAO.java
 * ─────────────────────────────────────────────────────
 * Handles all database operations for the `payments` table.
 * Also updates the `orders` table status when payment completes.
 *
 
 * If DBConnection isn't ready yet, use the inline getConnection()
 * method provided at the bottom of this file.
 * ─────────────────────────────────────────────────────
 * Author : Samuel (Payment Module)
 */
public class PaymentDAO {

    // ── 1. Insert a new payment record (status = Pending) ──
    public int insertPayment(Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (order_id, amount, payment_method, "
                   + "transaction_code, payment_status, payment_date) "
                   + "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt   (1, payment.getOrderId());
            ps.setDouble(2, payment.getAmount());
            ps.setString(3, payment.getPaymentMethod());
            ps.setString(4, payment.getTransactionCode());
            ps.setString(5, payment.getPaymentStatus());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);  // return new payment_id
        }
        return -1;
    }

    // ── 2. Update payment status by CheckoutRequestID ──────
    //    Called from MpesaCallbackServlet when Safaricom sends the result
    public boolean updatePaymentStatus(String checkoutRequestId,
                                       String newStatus,
                                       String mpesaReceiptNumber) throws SQLException {
        String sql = "UPDATE payments SET payment_status = ?, transaction_code = ? "
                   + "WHERE transaction_code = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            // Replace CheckoutRequestID with the real M-Pesa receipt number
            ps.setString(2, mpesaReceiptNumber != null ? mpesaReceiptNumber : checkoutRequestId);
            ps.setString(3, checkoutRequestId);

            return ps.executeUpdate() > 0;
        }
    }

    // ── 3. Update order status when payment completes ──────
    //    Keeps orders table in sync with payment outcome
    public boolean updateOrderStatus(int orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);  // e.g. "confirmed" or "failed"
            ps.setInt   (2, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── 4. Fetch a payment by order_id ────────────────────
    public Payment getPaymentByOrderId(int orderId) throws SQLException {
        String sql = "SELECT * FROM payments WHERE order_id = ? ORDER BY payment_date DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Payment p = new Payment();
                p.setPaymentId      (rs.getInt      ("payment_id"));
                p.setOrderId        (rs.getInt      ("order_id"));
                p.setAmount         (rs.getDouble   ("amount"));
                p.setPaymentMethod  (rs.getString   ("payment_method"));
                p.setTransactionCode(rs.getString   ("transaction_code"));
                p.setPaymentStatus  (rs.getString   ("payment_status"));
                p.setPaymentDate    (rs.getTimestamp("payment_date"));
                return p;
            }
        }
        return null;
    }

    // ── 5. Fetch order amount from orders table ───────────
    public double getOrderAmount(int orderId) throws SQLException {
        String sql = "SELECT total_amount FROM orders WHERE order_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("total_amount");
        }
        return 0.0;
    }

    // ── Database connection ───────────────────────────────
 
    //    shared utility class is available in the project.
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
        // Update these credentials to match your local MySQL setup
        return DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/dairy_sales_db",
            "root",      // your MySQL username
            "admin"       // your MySQL password
        );
    }
}
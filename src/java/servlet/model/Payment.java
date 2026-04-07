package servlet.model;

import java.sql.Timestamp;

/**
 * Payment.java
 * ─────────────────────────────────────────────────────
 * Model class that mirrors the `payments` table in MySQL.
 *
 * payments table schema:
 *   payment_id      INT PK AUTO_INCREMENT
 *   order_id        INT FK → orders
 *   amount          DECIMAL(10,2)
 *   payment_method  VARCHAR(50)
 *   transaction_code VARCHAR(100)   ← M-Pesa MpesaReceiptNumber / CheckoutRequestID
 *   payment_status  ENUM('Pending','Completed','Failed','Cancelled')
 *   payment_date    TIMESTAMP
 * ─────────────────────────────────────────────────────
 * Author : Samuel (Payment Module)
 */
public class Payment {

    private int       paymentId;
    private int       orderId;
    private double    amount;
    private String    paymentMethod;    // e.g. "M-Pesa"
    private String    transactionCode;  // CheckoutRequestID initially, then MpesaReceiptNumber
    private String    paymentStatus;    // Pending | Completed | Failed | Cancelled
    private Timestamp paymentDate;

    // ── Constructors ──────────────────────────────────

    public Payment() {}

    public Payment(int orderId, double amount, String paymentMethod,
                   String transactionCode, String paymentStatus) {
        this.orderId         = orderId;
        this.amount          = amount;
        this.paymentMethod   = paymentMethod;
        this.transactionCode = transactionCode;
        this.paymentStatus   = paymentStatus;
    }

    // ── Getters & Setters ─────────────────────────────

    public int getPaymentId()                      { return paymentId; }
    public void setPaymentId(int paymentId)        { this.paymentId = paymentId; }

    public int getOrderId()                        { return orderId; }
    public void setOrderId(int orderId)            { this.orderId = orderId; }

    public double getAmount()                      { return amount; }
    public void setAmount(double amount)           { this.amount = amount; }

    public String getPaymentMethod()               { return paymentMethod; }
    public void setPaymentMethod(String m)         { this.paymentMethod = m; }

    public String getTransactionCode()             { return transactionCode; }
    public void setTransactionCode(String code)    { this.transactionCode = code; }

    public String getPaymentStatus()               { return paymentStatus; }
    public void setPaymentStatus(String status)    { this.paymentStatus = status; }

    public Timestamp getPaymentDate()              { return paymentDate; }
    public void setPaymentDate(Timestamp ts)       { this.paymentDate = ts; }

    @Override
    public String toString() {
        return "Payment{orderId=" + orderId + ", amount=" + amount
                + ", status=" + paymentStatus + ", txCode=" + transactionCode + "}";
    }
}
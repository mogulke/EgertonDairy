package servlet;

import dao.PaymentDAO;
import model.Payment;
import service.MpesaConfig;
import service.MpesaService;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * PaymentServlet.java
 * ─────────────────────────────────────────────────────
 * Mapped to: /pay
 *
 * GET  /pay?orderId=X  → show payment.jsp (prefilled with order amount)
 * POST /pay            → validate form, initiate M-Pesa STK Push,
 *                        insert Pending payment, redirect to status page
 *
 * Session requirement:
 *   user must be logged in (Risper's LoginServlet sets session attribute "user")
 * ─────────────────────────────────────────────────────
 * Author : Samuel (Payment Module)
 */
@WebServlet("/pay")
public class PaymentServlet extends HttpServlet {

    private final MpesaService mpesaService = new MpesaService();
    private final PaymentDAO   paymentDAO   = new PaymentDAO();

    // ── GET: Load the payment page ────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Guard: must be logged in
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String orderIdParam = req.getParameter("orderId");
        if (orderIdParam == null || orderIdParam.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/products.jsp");
            return;
        }

        try {
            int orderId = Integer.parseInt(orderIdParam);
            double amount = paymentDAO.getOrderAmount(orderId);

            req.setAttribute("orderId", orderId);
            req.setAttribute("amount",  amount);
            req.getRequestDispatcher("/payment.jsp").forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/products.jsp");
        } catch (SQLException e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Could not load order details. Please try again.");
            req.getRequestDispatcher("/payment.jsp").forward(req, resp);
        }
    }

    // ── POST: Process payment ─────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Guard: must be logged in
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        String orderIdParam = req.getParameter("orderId");
        String phoneParam   = req.getParameter("phone");
        String amountParam  = req.getParameter("amount");

        // ── Basic validation ──────────────────────────
        if (orderIdParam == null || phoneParam == null || amountParam == null
                || orderIdParam.isBlank() || phoneParam.isBlank() || amountParam.isBlank()) {
            req.setAttribute("errorMessage", "All fields are required.");
            req.getRequestDispatcher("/payment.jsp").forward(req, resp);
            return;
        }

        int    orderId = Integer.parseInt(orderIdParam);
        double amount  = Double.parseDouble(amountParam);
        String phone   = mpesaService.normalizePhone(phoneParam);

        // Phone must be 12 digits (254XXXXXXXXX)
        if (!phone.matches("^254\\d{9}$")) {
            req.setAttribute("errorMessage",
                "Invalid phone number. Enter a valid Safaricom number (e.g. 0712345678).");
            req.setAttribute("orderId", orderId);
            req.setAttribute("amount",  amount);
            req.getRequestDispatcher("/payment.jsp").forward(req, resp);
            return;
        }

        try {
            String checkoutRequestId;

            // ── SIMULATION MODE ───────────────────────
            if (MpesaConfig.SIMULATE) {
                // Auto-generate a fake transaction code and mark as completed
                checkoutRequestId = "SIM-" + System.currentTimeMillis();

                Payment payment = new Payment(
                    orderId, amount, "M-Pesa (Simulated)", checkoutRequestId, "Completed");
                paymentDAO.insertPayment(payment);
                paymentDAO.updateOrderStatus(orderId, "confirmed");

                session.setAttribute("paymentStatus",  "Completed");
                session.setAttribute("transactionCode", checkoutRequestId);
                session.setAttribute("paymentOrderId",  orderId);
                resp.sendRedirect(req.getContextPath() + "/paymentStatus.jsp");
                return;
            }

            // ── REAL M-PESA STK PUSH ──────────────────
            checkoutRequestId = mpesaService.initiateStkPush(phone, (int) Math.ceil(amount), orderId);

            if (checkoutRequestId == null) {
                req.setAttribute("errorMessage",
                    "M-Pesa request failed. Check your credentials or network, then try again.");
                req.setAttribute("orderId", orderId);
                req.setAttribute("amount",  amount);
                req.getRequestDispatcher("/payment.jsp").forward(req, resp);
                return;
            }

            // Insert payment as Pending — callback will update it to Completed/Failed
            Payment payment = new Payment(orderId, amount, "M-Pesa", checkoutRequestId, "Pending");
            paymentDAO.insertPayment(payment);

            // Store in session so paymentStatus.jsp can poll / display it
            session.setAttribute("paymentStatus",   "Pending");
            session.setAttribute("transactionCode",  checkoutRequestId);
            session.setAttribute("paymentOrderId",   orderId);
            session.setAttribute("paymentPhone",     phone);

            resp.sendRedirect(req.getContextPath() + "/paymentStatus.jsp");

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Unexpected error: " + e.getMessage());
            req.setAttribute("orderId", orderId);
            req.setAttribute("amount",  amount);
            req.getRequestDispatcher("/payment.jsp").forward(req, resp);
        }
    }
}
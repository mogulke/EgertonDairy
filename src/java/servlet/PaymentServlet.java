package servlet;

import servlet.dao.PaymentDAO;
import servlet.model.Payment;
import servlet.service.MpesaConfig;
import servlet.service.MpesaService;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * PaymentServlet.java
 * Mapped to: /pay
 * GET  /pay?orderId=X  → populates Payment bean → forwards to Payment.jsp
 * POST /pay            → validates form → initiates M-Pesa STK Push
 * Author: Samuel (Payment Module)
 */
@WebServlet("/pay")
public class PaymentServlet extends HttpServlet {

    private MpesaService mpesaService;
    private PaymentDAO   paymentDAO;

    @Override
    public void init() throws ServletException {
        mpesaService = new MpesaService();
        paymentDAO   = new PaymentDAO();
    }

    // ── GET: Load the payment page ────────────────────
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Guard: must be logged in (uncomment when Risper's login is ready)
        // HttpSession session = req.getSession(false);
        // if (session == null || session.getAttribute("user") == null) {
        //     resp.sendRedirect(req.getContextPath() + "/login.jsp");
        //     return;
        // }

        String orderIdParam = req.getParameter("orderId");
        if (orderIdParam == null || orderIdParam.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/products.jsp");
            return;
        }

        try {
            int    orderId = Integer.parseInt(orderIdParam);
            double amount  = 1440.00; // test value — replace with paymentDAO.getOrderAmount(orderId) when DB is ready

            // ── Create Payment Java Bean and set its properties ──
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(amount);
            payment.setPaymentMethod("M-Pesa");
            payment.setPaymentStatus("Pending");

            // ── Calculate subtotal and store for EL ──────────────
            double deliveryFee = 150.00;
            double subtotal    = amount - deliveryFee;
            if (subtotal < 0) subtotal = amount;

            // ── Put bean and values in request scope ─────────────
            // JSP reads these via EL: ${payment.orderId}, ${subtotal} etc.
            req.setAttribute("payment",     payment);
            req.setAttribute("deliveryFee", deliveryFee);
            req.setAttribute("subtotal",    subtotal);

            req.getRequestDispatcher("/Payment.jsp").forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/products.jsp");
        }
    }

    // ── POST: Process payment ─────────────────────────
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(true);

        String orderIdParam = req.getParameter("orderId");
        String phoneParam   = req.getParameter("phone");
        String amountParam  = req.getParameter("amount");

        // ── Validation ────────────────────────────────
        if (orderIdParam == null || phoneParam == null || amountParam == null
                || orderIdParam.isBlank() || phoneParam.isBlank() || amountParam.isBlank()) {
            req.setAttribute("errorMessage", "All fields are required.");
            req.getRequestDispatcher("/Payment.jsp").forward(req, resp);
            return;
        }

        int    orderId = Integer.parseInt(orderIdParam);
        double amount  = Double.parseDouble(amountParam);
        String phone   = mpesaService.normalizePhone(phoneParam);

        if (!phone.matches("^254\\d{9}$")) {
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setAmount(amount);
            double deliveryFee = 150.00;
            req.setAttribute("payment",      payment);
            req.setAttribute("deliveryFee",  deliveryFee);
            req.setAttribute("subtotal",     amount - deliveryFee);
            req.setAttribute("errorMessage", "Invalid phone number. Enter a valid Safaricom number e.g. 0712345678.");
            req.getRequestDispatcher("/Payment.jsp").forward(req, resp);
            return;
        }

        try {
            String checkoutRequestId;

            // ── SIMULATION MODE ───────────────────────
            if (MpesaConfig.SIMULATE) {
                checkoutRequestId = "SIM-" + System.currentTimeMillis();

                Payment payment = new Payment(
                    orderId, amount, "M-Pesa (Simulated)", checkoutRequestId, "Completed");
                paymentDAO.insertPayment(payment);
                paymentDAO.updateOrderStatus(orderId, "confirmed");

                session.setAttribute("paymentStatus",   "Completed");
                session.setAttribute("transactionCode", checkoutRequestId);
                session.setAttribute("paymentOrderId",  orderId);
                resp.sendRedirect(req.getContextPath() + "/Paymentstatus.jsp");
                return;
            }

            // ── REAL M-PESA STK PUSH ──────────────────
            checkoutRequestId = mpesaService.initiateStkPush(phone, (int) Math.ceil(amount), orderId);

            if (checkoutRequestId == null) {
                Payment payment = new Payment();
                payment.setOrderId(orderId);
                payment.setAmount(amount);
                double deliveryFee = 150.00;
                req.setAttribute("payment",      payment);
                req.setAttribute("deliveryFee",  deliveryFee);
                req.setAttribute("subtotal",     amount - deliveryFee);
                req.setAttribute("errorMessage", "M-Pesa request failed. Check credentials or network and try again.");
                req.getRequestDispatcher("/Payment.jsp").forward(req, resp);
                return;
            }

            Payment payment = new Payment(orderId, amount, "M-Pesa", checkoutRequestId, "Pending");
            paymentDAO.insertPayment(payment);

            session.setAttribute("paymentStatus",   "Pending");
            session.setAttribute("transactionCode",  checkoutRequestId);
            session.setAttribute("paymentOrderId",   orderId);
            session.setAttribute("paymentPhone",     phone);

            resp.sendRedirect(req.getContextPath() + "/Paymentstatus.jsp");

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Unexpected error: " + e.getMessage());
            req.getRequestDispatcher("/Payment.jsp").forward(req, resp);
        }
    }
}

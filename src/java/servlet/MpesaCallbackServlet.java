package servlet;

import dao.PaymentDAO;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * MpesaCallbackServlet.java
 * ─────────────────────────────────────────────────────
 * Mapped to: /mpesa/callback
 * (matches MpesaConfig.CALLBACK_URL path)
 *
 * Safaricom calls this URL asynchronously after the customer
 * approves or declines the STK Push on their phone.
 *
 * Callback JSON structure (success):
 * {
 *   "Body": {
 *     "stkCallback": {
 *       "ResultCode": 0,
 *       "CheckoutRequestID": "ws_CO_...",
 *       "CallbackMetadata": {
 *         "Item": [
 *           { "Name":"Amount",            "Value":500 },
 *           { "Name":"MpesaReceiptNumber","Value":"QBK34VY..." },
 *           { "Name":"PhoneNumber",       "Value":254712... }
 *         ]
 *       }
 *     }
 *   }
 * }
 *
 * Callback JSON structure (failure / user cancelled):
 * {
 *   "Body": {
 *     "stkCallback": {
 *       "ResultCode": 1032,   (1032 = cancelled by user)
 *       "CheckoutRequestID": "ws_CO_..."
 *     }
 *   }
 * }
 * ─────────────────────────────────────────────────────
 * Author : Samuel (Payment Module)
 */
@WebServlet("/mpesa/callback")
public class MpesaCallbackServlet extends HttpServlet {

    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Read raw JSON body from Safaricom
        String body = req.getReader().lines()
                        .collect(Collectors.joining("\n"));

        System.out.println("[MpesaCallback] Received:\n" + body);

        // Always respond 200 immediately so Safaricom doesn't retry
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"ResultCode\":0,\"ResultDesc\":\"Accepted\"}");

        // Parse and process in the background
        try {
            processCallback(body);
        } catch (Exception e) {
            e.printStackTrace();
            // Don't throw — we already sent 200 to Safaricom
        }
    }

    private void processCallback(String json) throws SQLException {
        // Extract ResultCode
        String resultCodeStr = extractJsonValue(json, "ResultCode");
        int resultCode = resultCodeStr != null ? Integer.parseInt(resultCodeStr.trim()) : -1;

        // Extract CheckoutRequestID
        String checkoutRequestId = extractJsonValue(json, "CheckoutRequestID");
        if (checkoutRequestId == null) {
            System.err.println("[MpesaCallback] No CheckoutRequestID in callback body.");
            return;
        }

        if (resultCode == 0) {
            // ── SUCCESS ──────────────────────────────
            String mpesaReceipt = extractJsonValue(json, "MpesaReceiptNumber");

            // Update payment: Pending → Completed, store real receipt number
            boolean updated = paymentDAO.updatePaymentStatus(
                    checkoutRequestId, "Completed", mpesaReceipt);

            if (updated) {
                // Also update the order status to "confirmed"
                // We need the orderId — fetch payment record to get it
                // (Alternative: PaymentDAO.getPaymentByTransactionCode — add if needed)
                System.out.println("[MpesaCallback] Payment completed. Receipt: " + mpesaReceipt);
            }

        } else {
            // ── FAILURE / CANCELLED ───────────────────
            // ResultCode 1032 = cancelled by user
            // ResultCode 1037 = timeout
            System.out.println("[MpesaCallback] Payment failed. ResultCode: " + resultCode);
            paymentDAO.updatePaymentStatus(checkoutRequestId, "Failed", null);
        }
    }

    /**
     * Minimal JSON value extractor — same as in MpesaService.
     * Extracts first occurrence of "key":"value" from flat or nested JSON.
     */
    private String extractJsonValue(String json, String key) {
        // Try string value: "Key":"Value"
        String searchStr = "\"" + key + "\":\"";
        int start = json.indexOf(searchStr);
        if (start != -1) {
            start += searchStr.length();
            int end = json.indexOf("\"", start);
            return end == -1 ? null : json.substring(start, end);
        }
        // Try numeric value: "Key":123
        String searchNum = "\"" + key + "\":";
        start = json.indexOf(searchNum);
        if (start != -1) {
            start += searchNum.length();
            // skip whitespace
            while (start < json.length() && json.charAt(start) == ' ') start++;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end))
                    || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            return json.substring(start, end);
        }
        return null;
    }
}
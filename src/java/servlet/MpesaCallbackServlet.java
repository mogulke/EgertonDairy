package servlet;

import servlet.dao.PaymentDAO;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MpesaCallbackServlet.java
 * ─────────────────────────────────────────────────────
 * Mapped to: /mpesa/callback
 *
 * Safaricom calls this URL after customer approves/declines STK Push.
 *
 * The result is stored in a static ConcurrentHashMap (paymentResults).
 * Paymentstatus.jsp reads from this map on each auto-refresh to check
 * if the payment has completed — this is necessary because Safaricom's
 * callback arrives on a different thread with no access to browser sessions.
 *
 * Map key   → CheckoutRequestID (e.g. "ws_CO_09042026...")
 * Map value → "Completed:QBK34VY..." or "Failed"
 * ─────────────────────────────────────────────────────
 * Author: Samuel (Payment Module)
 */
@WebServlet("/mpesa/callback")
public class MpesaCallbackServlet extends HttpServlet {

    /**
     * Static map shared across all servlet instances.
     * Paymentstatus.jsp reads: MpesaCallbackServlet.paymentResults.get(checkoutId)
     */
    public static final ConcurrentHashMap<String, String> paymentResults
            = new ConcurrentHashMap<>();

    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Read raw JSON body sent by Safaricom
        String body = req.getReader().lines()
                        .collect(Collectors.joining("\n"));

        System.out.println("[MpesaCallback] Received:\n" + body);

        // Always respond 200 immediately — Safaricom will retry if we don't
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"ResultCode\":0,\"ResultDesc\":\"Accepted\"}");

        // Process result after responding
        try {
            processCallback(body);
        } catch (Exception e) {
            e.printStackTrace();
            // Never throw — we already responded 200 to Safaricom
        }
    }

    private void processCallback(String json) {
        String resultCodeStr     = extractJsonValue(json, "ResultCode");
        String checkoutRequestId = extractJsonValue(json, "CheckoutRequestID");

        int resultCode = resultCodeStr != null ? Integer.parseInt(resultCodeStr.trim()) : -1;

        System.out.println("[MpesaCallback] ResultCode: " + resultCode);
        System.out.println("[MpesaCallback] CheckoutRequestID: " + checkoutRequestId);

        if (checkoutRequestId == null) {
            System.out.println("[MpesaCallback] ERROR — No CheckoutRequestID found.");
            return;
        }

        if (resultCode == 0) {
            // ── SUCCESS ──────────────────────────────────────
            String mpesaReceipt = extractJsonValue(json, "MpesaReceiptNumber");
            System.out.println("[MpesaCallback] SUCCESS — Receipt: " + mpesaReceipt);

            // Store in map so Paymentstatus.jsp can read it on next refresh
            paymentResults.put(checkoutRequestId,
                    "Completed:" + (mpesaReceipt != null ? mpesaReceipt : ""));

            // Update DB — Pending → Completed (best effort, DB may not be set up yet)
            try {
                paymentDAO.updatePaymentStatus(checkoutRequestId, "Completed", mpesaReceipt);
            } catch (Exception e) {
                System.out.println("[MpesaCallback] DB update skipped (DB not ready): " + e.getMessage());
            }

        } else {
            // ── FAILURE / CANCELLED ───────────────────────────
            // 1032 = cancelled by user, 1037 = timeout
            System.out.println("[MpesaCallback] FAILED — ResultCode: " + resultCode);

            paymentResults.put(checkoutRequestId, "Failed");

            try {
                paymentDAO.updatePaymentStatus(checkoutRequestId, "Failed", null);
            } catch (Exception e) {
                System.out.println("[MpesaCallback] DB update skipped: " + e.getMessage());
            }
        }
    }

    /**
     * Extracts a value from JSON for both:
     *   "Key":"Value"   (string)
     *   "Key":123       (number)
     */
    private String extractJsonValue(String json, String key) {
        // String value
        String searchStr = "\"" + key + "\":\"";
        int start = json.indexOf(searchStr);
        if (start != -1) {
            start += searchStr.length();
            int end = json.indexOf("\"", start);
            return end == -1 ? null : json.substring(start, end);
        }
        // Numeric value
        String searchNum = "\"" + key + "\":";
        start = json.indexOf(searchNum);
        if (start != -1) {
            start += searchNum.length();
            while (start < json.length() && json.charAt(start) == ' ') start++;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end))
                    || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
            return json.substring(start, end);
        }
        return null;
    }
}

package servlet.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * MpesaService.java
 * ─────────────────────────────────────────────────────
 * Handles all communication with Safaricom Daraja API.
 *
 * Flow:
 *   1. getOAuthToken()    → gets a bearer token (valid 1 hour)
 *   2. initiateStkPush()  → sends STK push to customer's phone
 *   3. Safaricom calls your CALLBACK_URL with result
 *   4. MpesaCallbackServlet parses callback → updates DB
 *
 * Returns from initiateStkPush():
 *   On success → CheckoutRequestID (e.g. "ws_CO_260520...")
 *   On failure → null
 * ─────────────────────────────────────────────────────
 * Author : Samuel (Payment Module)
 */
public class MpesaService {

    /**
     * Step 1: Get OAuth access token from Safaricom.
     * Token expires after 3600 seconds — generate fresh per request
     * (for a school project, this is fine; production would cache it).
     */
    public String getOAuthToken() throws IOException {
        String credentials = MpesaConfig.CONSUMER_KEY + ":" + MpesaConfig.CONSUMER_SECRET;
        String encoded = Base64.getEncoder()
                               .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        URL url = new URL(MpesaConfig.OAUTH_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + encoded);
        conn.setRequestProperty("Content-Type", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            String response = readResponse(conn.getInputStream());
            // Response: {"access_token":"XXXX","expires_in":"3599"}
            return extractJsonValue(response, "access_token");
        } else {
            String errorBody = readResponse(conn.getErrorStream());
            System.err.println("[MpesaService] OAuth failed (" + responseCode + "): " + errorBody);
            return null;
        }
    }

    /**
     * Step 2: Initiate STK Push (Lipa Na M-Pesa Online).
     * Sends a payment prompt to the customer's phone.
     *
     * @param phone     Customer phone in format 2547XXXXXXXX
     * @param amount    Amount in KES (whole number, e.g. 500)
     * @param orderId   Your internal order ID (used in AccountReference)
     * @return          CheckoutRequestID to track this transaction, or null on failure
     */
    public String initiateStkPush(String phone, int amount, int orderId) throws IOException {
        String token = getOAuthToken();
        if (token == null) {
            System.err.println("[MpesaService] Cannot proceed — OAuth token is null.");
            return null;
        }

        // Generate timestamp: YYYYMMDDHHmmss
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Password = Base64(Shortcode + Passkey + Timestamp)
        String rawPassword = MpesaConfig.SHORTCODE + MpesaConfig.PASSKEY + timestamp;
        String password = Base64.getEncoder()
                                .encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));

        // Normalize phone: strip leading 0 or + if user typed it
        phone = normalizePhone(phone);

        // Build JSON request body
        String jsonBody = "{"
            + "\"BusinessShortCode\": \"" + MpesaConfig.SHORTCODE          + "\","
            + "\"Password\": \""           + password                       + "\","
            + "\"Timestamp\": \""          + timestamp                      + "\","
            + "\"TransactionType\": \""    + MpesaConfig.TRANSACTION_TYPE   + "\","
            + "\"Amount\": "               + amount                         + ","
            + "\"PartyA\": \""             + phone                          + "\","
            + "\"PartyB\": \""             + MpesaConfig.SHORTCODE          + "\","
            + "\"PhoneNumber\": \""        + phone                          + "\","
            + "\"CallBackURL\": \""        + MpesaConfig.CALLBACK_URL       + "\","
            + "\"AccountReference\": \""   + MpesaConfig.ACCOUNT_REFERENCE
                                           + "-" + orderId                  + "\","
            + "\"TransactionDesc\": \""    + MpesaConfig.TRANSACTION_DESC   + "\""
            + "}";

        URL url = new URL(MpesaConfig.STK_PUSH_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        String response  = responseCode == 200
                ? readResponse(conn.getInputStream())
                : readResponse(conn.getErrorStream());

        System.out.println("[MpesaService] STK Push response (" + responseCode + "): " + response);

        if (responseCode == 200) {
            return extractJsonValue(response, "CheckoutRequestID");
        } else {
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────

    /** Reads the full response body from a stream. */
    private String readResponse(InputStream stream) throws IOException {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    /**
     * Minimal JSON value extractor — avoids pulling in a full JSON library.
     * Works for flat string values like: {"key":"value",...}
     */
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    /**
     * Normalize phone to 2547XXXXXXXX format.
     * Handles: 0712345678, +254712345678, 254712345678
     */
    public String normalizePhone(String phone) {
        if (phone == null) return "";
        phone = phone.trim().replaceAll("\\s+", "");
        if (phone.startsWith("+")) phone = phone.substring(1);
        if (phone.startsWith("0")) phone = "254" + phone.substring(1);
        return phone;
    }
}
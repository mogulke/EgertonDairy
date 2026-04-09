package servlet.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * MpesaService.java
 * Handles all communication with Safaricom Daraja API.
 * Author: Samuel (Payment Module)
 */
public class MpesaService {

    /**
     * Step 1: Get OAuth access token from Safaricom.
     */
    public String getOAuthToken() throws IOException {

        System.out.println("[OAuth] Starting OAuth request...");
        System.out.println("[OAuth] Consumer Key starts with: " + MpesaConfig.CONSUMER_KEY.substring(0, 6) + "...");

        String credentials = MpesaConfig.CONSUMER_KEY + ":" + MpesaConfig.CONSUMER_SECRET;
        String encoded = Base64.getEncoder()
                               .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        System.out.println("[OAuth] Connecting to: " + MpesaConfig.OAUTH_URL);

        URL url = new URL(MpesaConfig.OAUTH_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + encoded);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000); // 10 seconds
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        System.out.println("[OAuth] Response code: " + responseCode);

        if (responseCode == 200) {
            String response = readResponse(conn.getInputStream());
            System.out.println("[OAuth] SUCCESS — token received.");
            return extractJsonValue(response, "access_token");
        } else {
            String errorBody = readResponse(conn.getErrorStream());
            System.out.println("[OAuth] FAILED — error: " + errorBody);
            return null;
        }
    }

    /**
     * Step 2: Initiate STK Push (Lipa Na M-Pesa Online).
     */
    public String initiateStkPush(String phone, int amount, int orderId) throws IOException {

        System.out.println("[STK] Starting STK Push...");
        System.out.println("[STK] Phone: " + phone + " | Amount: " + amount + " | OrderId: " + orderId);

        String token = getOAuthToken();
        if (token == null) {
            System.out.println("[STK] STOPPED — OAuth token is null.");
            return null;
        }

        System.out.println("[STK] OAuth token received, building STK request...");

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String rawPassword = MpesaConfig.SHORTCODE + MpesaConfig.PASSKEY + timestamp;
        String password = Base64.getEncoder()
                                .encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));

        phone = normalizePhone(phone);
        System.out.println("[STK] Normalized phone: " + phone);

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

        System.out.println("[STK] Sending request to Safaricom...");
        System.out.println("[STK] Callback URL: " + MpesaConfig.CALLBACK_URL);

        URL url = new URL(MpesaConfig.STK_PUSH_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        String response  = responseCode == 200
                ? readResponse(conn.getInputStream())
                : readResponse(conn.getErrorStream());

        System.out.println("[STK] Response code: " + responseCode);
        System.out.println("[STK] Response body: " + response);

        if (responseCode == 200) {
            String checkoutId = extractJsonValue(response, "CheckoutRequestID");
            System.out.println("[STK] SUCCESS — CheckoutRequestID: " + checkoutId);
            return checkoutId;
        } else {
            System.out.println("[STK] FAILED — see response body above.");
            return null;
        }
    }

    // ── Helpers ──────────────────────────────────────

    private String readResponse(InputStream stream) throws IOException {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private String extractJsonValue(String json, String key) {
    // Handle both "key":"value" and "key": "value" (with space)
    String search = "\"" + key + "\":\"";
    int start = json.indexOf(search);
    if (start == -1) {
        // Try with space after colon
        search = "\"" + key + "\": \"";
        start = json.indexOf(search);
    }
    if (start == -1) return null;
    start += search.length();
    int end = json.indexOf("\"", start);
    return end == -1 ? null : json.substring(start, end);
}

    public String normalizePhone(String phone) {
        if (phone == null) return "";
        phone = phone.trim().replaceAll("\\s+", "");
        if (phone.startsWith("+")) phone = phone.substring(1);
        if (phone.startsWith("0")) phone = "254" + phone.substring(1);
        return phone;
    }
}
package servlet.service;

/**
 * MpesaConfig.java
 * ─────────────────────────────────────────────────────
 * Central configuration for Safaricom Daraja API.
 *
 * HOW TO USE:
 *  1. Register at https://developer.safaricom.co.ke
 *  2. Create an app → get Consumer Key & Consumer Secret
 *  3. Use "Lipa Na M-Pesa Online" sandbox shortcode: 174379
 *  4. Replace the values below with your credentials
 *  5. For callbacks, use ngrok: ngrok http 8080
 *     then set CALLBACK_URL to your ngrok URL + /DairySales/mpesa/callback
 *
 * SIMULATION MODE:
 *  Set SIMULATE = true during offline development.
 *  The system will auto-approve every payment without calling Safaricom.
 * ─────────────────────────────────────────────────────
 * Author : Samuel (Payment Module)
 * Project: Dairy Sales & E-Commerce Management System
 */
public class MpesaConfig {

    // ── Toggle simulation mode ────────────────────────
    public static final boolean SIMULATE = false;  //false for real M-Pesa

    // ── Daraja API credentials (Sandbox) ─────────────
    public static final String CONSUMER_KEY    = "OVuJ2L3dZXKghARmXRRalMMBAxr1KGPfnFTOsALRLbJEKckB";  
    public static final String CONSUMER_SECRET = "zJDR4NWWjWlRkwEqArWe19HO7EGCRSzmTJvH29iSpz4qn2wKOOPNyqot5Z26lkW1";  

    // ── Lipa Na M-Pesa Online (STK Push) ─────────────
    public static final String SHORTCODE         = "174379";
    public static final String PASSKEY           = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919";
    public static final String TRANSACTION_TYPE  = "CustomerPayBillOnline";
    public static final String ACCOUNT_REFERENCE = "DairySales";
    public static final String TRANSACTION_DESC  = "Dairy Product Purchase";

    // ── callback URL (ngrok in development) ──
    // Example: "https://abc123.ngrok.io/DairySales/mpesa/callback"
    public static final String CALLBACK_URL  = "https://58d1-2c0f-fe38-2411-d89c-cdda-171-8d76-edec.ngrok-free.app/DairySales/mpesa/callback";

    // ── Daraja API endpoints (Sandbox) ───────────────
    public static final String OAUTH_URL     = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
    public static final String STK_PUSH_URL  = "https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest";

    /*
     * For production, replace sandbox URLs with:
     * OAUTH_URL    = "https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
     * STK_PUSH_URL = "https://api.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
     * And use your real shortcode + passkey from the Safaricom portal.
     */
}
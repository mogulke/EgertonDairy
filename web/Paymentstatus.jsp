<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="javax.servlet.http.HttpSession" %>
<%
    /* Redirect to login if not logged in */
    HttpSession sess = request.getSession(false);
    if (sess == null || sess.getAttribute("user") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }

    String paymentStatus   = (String)  sess.getAttribute("paymentStatus");
    String transactionCode = (String)  sess.getAttribute("transactionCode");
    Object orderIdObj      = sess.getAttribute("paymentOrderId");
    int    orderId         = orderIdObj != null ? (Integer) orderIdObj : 0;

    if (paymentStatus == null) paymentStatus = "Pending";

    boolean isCompleted = "Completed".equalsIgnoreCase(paymentStatus);
    boolean isFailed    = "Failed".equalsIgnoreCase(paymentStatus)
                       || "Cancelled".equalsIgnoreCase(paymentStatus);
    boolean isPending   = "Pending".equalsIgnoreCase(paymentStatus);

    /* Generate a short order reference number */
    String orderRef = "EDS-" + (10000 + orderId);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>
        <%= isCompleted ? "Order Confirmed" : isPending ? "Payment Pending" : "Payment Failed" %>
        | Dairy Sales System
    </title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css" rel="stylesheet">

    <%-- Auto-refresh every 8 seconds while payment is pending --%>
    <% if (isPending) { %>
    <meta http-equiv="refresh" content="8">
    <% } %>

    <style>
        body {
            background-color: #f2f2ec;
            font-family: Georgia, serif;
            color: #1a1a1a;
            margin: 0;
        }

        /* Navbar */
        .top-nav {
            background: #f2f2ec;
            border-bottom: 1px solid #ddd;
            padding: 14px 32px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .brand-name { font-size: 1rem; font-weight: 700; color: #1a3c2d; }
        .nav-status {
            font-size: 0.72rem;
            font-weight: 700;
            letter-spacing: 1.5px;
            text-transform: uppercase;
            font-family: 'Segoe UI', sans-serif;
        }
        .status-confirmed { color: #2d7a45; }
        .status-pending   { color: #b07d00; }
        .status-failed    { color: #c0392b; }

        /* Main container */
        .page-container {
            max-width: 860px;
            margin: 40px auto;
            padding: 0 24px 60px;
        }

        /* Icon circle */
        .icon-circle {
            width: 72px;
            height: 72px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.8rem;
            margin: 0 auto 20px;
        }
        .icon-success { background: #2d7a45; color: white; }
        .icon-pending { background: #f0c040; color: #7a5200; }
        .icon-failed  { background: #c0392b; color: white; }

        .status-heading {
            text-align: center;
            margin-bottom: 6px;
        }
        .status-heading h2 {
            font-size: 1.7rem;
            font-weight: 700;
        }
        .status-heading h2.text-success { color: #1a3c2d; }
        .status-heading p {
            color: #888;
            font-size: 0.85rem;
            font-family: 'Segoe UI', sans-serif;
            font-style: italic;
        }

        /* Two-column cards */
        .cards-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            margin-top: 30px;
        }
        .card-box {
            background: #fff;
            border-radius: 12px;
            padding: 24px;
            border: 1px solid #e0e0da;
        }
        .card-section-title {
            font-size: 0.78rem;
            font-weight: 700;
            color: #2d7a45;
            font-family: 'Segoe UI', sans-serif;
            letter-spacing: 0.3px;
            margin-bottom: 16px;
        }

        /* Delivery info rows */
        .info-label {
            font-size: 0.65rem;
            text-transform: uppercase;
            letter-spacing: 1px;
            color: #aaa;
            font-family: 'Segoe UI', sans-serif;
            margin-bottom: 3px;
        }
        .info-value {
            font-size: 0.88rem;
            font-family: 'Segoe UI', sans-serif;
            color: #1a1a1a;
            margin-bottom: 14px;
            display: flex;
            align-items: center;
            gap: 7px;
        }
        .info-value i { color: #2d7a45; font-size: 0.85rem; }

        /* Buttons */
        .btn-track {
            width: 100%;
            background: #2d7a45;
            color: white;
            border: none;
            border-radius: 8px;
            padding: 12px;
            font-size: 0.88rem;
            font-weight: 600;
            font-family: 'Segoe UI', sans-serif;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 7px;
            text-decoration: none;
            margin-bottom: 10px;
            transition: background 0.2s;
        }
        .btn-track:hover { background: #1d5c30; color: white; }
        .btn-home {
            width: 100%;
            background: #f5f5ef;
            color: #555;
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 11px;
            font-size: 0.85rem;
            font-family: 'Segoe UI', sans-serif;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 7px;
            text-decoration: none;
            transition: background 0.2s;
        }
        .btn-home:hover { background: #ebebeb; color: #1a1a1a; }

        /* Order items in summary */
        .order-item-row {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 12px;
        }
        .item-icon {
            width: 42px;
            height: 42px;
            border-radius: 7px;
            background: #f0f7f3;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.1rem;
            flex-shrink: 0;
        }
        .item-name {
            font-size: 0.84rem;
            font-weight: 600;
            line-height: 1.3;
            flex: 1;
        }
        .item-sub {
            font-size: 0.74rem;
            color: #aaa;
            font-family: 'Segoe UI', sans-serif;
        }
        .item-price {
            font-size: 0.84rem;
            font-weight: 600;
            color: #1a3c2d;
            white-space: nowrap;
        }
        .divider { border-top: 1px solid #eee; margin: 12px 0; }
        .totals-row {
            display: flex;
            justify-content: space-between;
            font-size: 0.82rem;
            font-family: 'Segoe UI', sans-serif;
            color: #888;
            margin-bottom: 5px;
        }
        .totals-final {
            display: flex;
            justify-content: space-between;
            font-size: 0.9rem;
            font-weight: 700;
            color: #1a3c2d;
            margin-top: 8px;
        }

        /* Pending / failed states */
        .full-status-card {
            background: #fff;
            border-radius: 12px;
            padding: 48px 32px;
            text-align: center;
            border: 1px solid #e0e0da;
            margin-top: 30px;
        }
        .pending-spinner {
            width: 48px;
            height: 48px;
            border: 4px solid #f0c040;
            border-top-color: transparent;
            border-radius: 50%;
            animation: spin 0.9s linear infinite;
            margin: 20px auto;
        }
        @keyframes spin { to { transform: rotate(360deg); } }

        /* Support link */
        .support-line {
            text-align: center;
            margin-top: 28px;
            font-size: 0.82rem;
            font-family: 'Segoe UI', sans-serif;
            color: #aaa;
        }
        .support-line a { color: #2d7a45; text-decoration: none; font-weight: 600; }

        /* Footer */
        footer {
            border-top: 1px solid #ddd;
            padding: 18px 32px;
            display: flex;
            justify-content: space-between;
            font-family: 'Segoe UI', sans-serif;
            font-size: 0.75rem;
            color: #aaa;
        }
        footer a { color: #aaa; text-decoration: none; margin-left: 16px; }
        footer a:hover { color: #1a3c2d; }

        @media (max-width: 620px) {
            .cards-grid { grid-template-columns: 1fr; }
            .page-container { padding: 0 14px 40px; }
        }
    </style>
</head>
<body>

<!-- Navbar -->
<nav class="top-nav">
    <span class="brand-name">EgertonDairySales</span>
    <% if (isCompleted) { %>
        <span class="nav-status status-confirmed">ORDER CONFIRMED</span>
    <% } else if (isPending) { %>
        <span class="nav-status status-pending">PAYMENT PENDING</span>
    <% } else { %>
        <span class="nav-status status-failed">PAYMENT FAILED</span>
    <% } %>
</nav>

<div class="page-container">

    <!-- ════════════════════════════════════════ -->
    <!--  SUCCESS STATE                           -->
    <!-- ════════════════════════════════════════ -->
    <% if (isCompleted) { %>

    <!-- Icon + heading -->
    <div style="text-align:center; margin-bottom: 4px;">
        <div class="icon-circle icon-success" style="margin-top: 12px;">
            <i class="bi bi-check-lg"></i>
        </div>
        <div class="status-heading">
            <h2 class="text-success">Order Placed Successfully!</h2>
            <p>Thank you for supporting modern pastoral excellence.</p>
        </div>
    </div>

    <!-- Two cards -->
    <div class="cards-grid">

        <!-- Delivery Information -->
        <div class="card-box">
            <div class="card-section-title">Delivery Information</div>

            <div class="info-label">Order Number</div>
            <div class="info-value">#<%= orderRef %></div>

            <div class="info-label">Estimated Delivery</div>
            <div class="info-value">
                <i class="bi bi-clock"></i> Today, 4:00 PM – 5:00 PM
            </div>

            <div class="info-label">Shipping To</div>
            <div class="info-value">Main Campus, Egerton University</div>

            <% if (transactionCode != null) { %>
            <div class="info-label">M-Pesa Receipt</div>
            <div class="info-value" style="font-size:0.8rem; color:#555;">
                <%= transactionCode %>
            </div>
            <% } %>

            <a href="<%= request.getContextPath() %>/orders.jsp" class="btn-track">
                <i class="bi bi-truck"></i> Track My Order
            </a>
            <a href="<%= request.getContextPath() %>/products.jsp" class="btn-home">
                <i class="bi bi-house"></i> Back to Home
            </a>
        </div>

        <!-- Order Summary -->
        <div class="card-box">
            <div class="card-section-title">Order Summary</div>

            <%--sample--%>

            <div class="order-item-row">
                <div class="item-icon">🥛</div>
                <div style="flex:1;">
                    <div class="item-name">Fresh Organic Milk (2L)</div>
                    <div class="item-sub">Qty: 2</div>
                </div>
                <div class="item-price">KES 440</div>
            </div>

            <div class="order-item-row">
                <div class="item-icon">🧀</div>
                <div style="flex:1;">
                    <div class="item-name">Egerton Gold Cheddar</div>
                    <div class="item-sub">500g</div>
                </div>
                <div class="item-price">KES 850</div>
            </div>

            <div class="divider"></div>

            <div class="totals-row">
                <span>Subtotal</span>
                <span>KES 1,290</span>
            </div>
            <div class="totals-row">
                <span>Delivery Fee</span>
                <span>KES 150</span>
            </div>

            <div class="divider"></div>

            <div class="totals-final">
                <span>Total Paid</span>
                <span>KES 1,440</span>
            </div>
        </div>
    </div>

    <!-- Support line -->
    <div class="support-line">
        Need help with your order? <a href="#">Contact Support</a>
    </div>


    <!-- ════════════════════════════════════════ -->
    <!--  PENDING STATE                           -->
    <!-- ════════════════════════════════════════ -->
    <% } else if (isPending) { %>

    <div class="full-status-card">
        <div class="icon-circle icon-pending" style="margin: 0 auto 16px;">
            <i class="bi bi-hourglass-split"></i>
        </div>
        <h4 style="font-weight:700; color:#7a5200;">Waiting for Payment...</h4>
        <p style="font-family:'Segoe UI',sans-serif; color:#888; font-size:0.88rem; margin-bottom:4px;">
            A payment prompt has been sent to your M-Pesa number.
        </p>
        <p style="font-family:'Segoe UI',sans-serif; color:#555; font-size:0.88rem;">
            <strong>Enter your PIN on your phone</strong> to complete the payment.
        </p>
        <div class="pending-spinner"></div>
        <p style="font-family:'Segoe UI',sans-serif; font-size:0.78rem; color:#bbb;">
            This page refreshes automatically every 8 seconds.
        </p>
        <% if (transactionCode != null) { %>
        <p style="font-family:'Segoe UI',sans-serif; font-size:0.72rem; color:#ccc; margin-top:8px;">
            Ref: <%= transactionCode %>
        </p>
        <% } %>
        <a href="<%= request.getContextPath() %>/products.jsp" class="btn-home" style="max-width:220px; margin: 16px auto 0;">
            Cancel & Go Back
        </a>
    </div>


    <!-- ════════════════════════════════════════ -->
    <!--  FAILED STATE                            -->
    <!-- ════════════════════════════════════════ -->
    <% } else { %>

    <div class="full-status-card">
        <div class="icon-circle icon-failed" style="margin: 0 auto 16px;">
            <i class="bi bi-x-lg"></i>
        </div>
        <h4 style="font-weight:700; color:#c0392b;">Payment Failed</h4>
        <p style="font-family:'Segoe UI',sans-serif; color:#888; font-size:0.88rem;">
            The payment was not completed. You may have cancelled it or it timed out.
        </p>
        <div style="display:flex; gap:12px; justify-content:center; margin-top:20px; flex-wrap:wrap;">
            <a href="<%= request.getContextPath() %>/pay?orderId=<%= orderId %>"
               class="btn-track" style="max-width:180px;">
                <i class="bi bi-arrow-repeat"></i> Try Again
            </a>
            <a href="<%= request.getContextPath() %>/products.jsp"
               class="btn-home" style="max-width:180px;">
                <i class="bi bi-house"></i> Back to Shop
            </a>
        </div>
    </div>

    <% } %>

</div>

<!-- Footer -->
<footer>
    <span>EgertonDairySales &copy; 2025 Egerton University. Modern Pastoral Excellence.</span>
    <div>
        <a href="#">Privacy Policy</a>
        <a href="#">Terms of Service</a>
        <a href="#">Shipping Info</a>
        <a href="#">Farmer Directory</a>
    </div>
</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

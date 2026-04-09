<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%-- ══════════════════════════════════════════════════════════
     Java Bean declaration — loads the Payment bean from
     request scope. The servlet already created and set it.
     EL expressions like ${payment.orderId} read from this bean.
     ══════════════════════════════════════════════════════════ --%>
<jsp:useBean id="payment" class="servlet.model.Payment" scope="request" />

<%-- JSTL tag libraries for <c:if> and <fmt:formatNumber> --%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Secure Checkout | Dairy Sales System</title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css" rel="stylesheet">

    <style>
        body {
            background-color: #f2f2ec;
            font-family: Georgia, serif;
            color: #1a1a1a;
            margin: 0;
        }
        .top-nav {
            background: #f2f2ec;
            border-bottom: 1px solid #ddd;
            padding: 14px 32px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .brand-name { font-size: 1rem; font-weight: 700; color: #1a3c2d; }
        .nav-links a {
            color: #555; text-decoration: none;
            font-size: 0.85rem; font-family: 'Segoe UI', sans-serif; margin-left: 20px;
        }
        .nav-links a:hover { color: #1a3c2d; }
        .page-heading { max-width: 1000px; margin: 0 auto; padding: 32px 32px 16px; }
        .page-heading h1 { font-size: 1.8rem; font-weight: 700; color: #1a3c2d; margin-bottom: 4px; }
        .page-heading p { color: #777; font-size: 0.88rem; font-family: 'Segoe UI', sans-serif; font-style: italic; margin: 0; }
        .main-grid {
            max-width: 1000px; margin: 0 auto; padding: 0 32px 60px;
            display: grid; grid-template-columns: 1fr 360px; gap: 24px;
        }
        .card-box { background: #fff; border-radius: 12px; padding: 28px; border: 1px solid #e0e0da; }
        .mpesa-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 22px; }
        .mpesa-label { font-size: 0.95rem; font-weight: 700; display: flex; align-items: center; gap: 8px; }
        .mpesa-label i { color: #2d7a45; }
        .mpesa-pill {
            background: #e6f4ec; color: #1a7a40; font-size: 0.72rem; font-weight: 700;
            padding: 4px 10px; border-radius: 20px; font-family: 'Segoe UI', sans-serif; letter-spacing: 0.5px;
        }
        .field-label {
            font-size: 0.7rem; font-weight: 700; letter-spacing: 1px; text-transform: uppercase;
            color: #999; font-family: 'Segoe UI', sans-serif; margin-bottom: 8px;
        }
        .phone-field {
            width: 100%; padding: 13px 16px; border: 1.5px solid #ccc; border-radius: 8px;
            font-size: 0.95rem; font-family: 'Segoe UI', sans-serif; background: #fafaf8; outline: none;
        }
        .phone-field:focus { border-color: #2d7a45; }
        .phone-hint { font-size: 0.78rem; color: #999; font-family: 'Segoe UI', sans-serif; margin-top: 8px; line-height: 1.5; }
        .btn-pay {
            width: 100%; background: #2d7a45; color: white; border: none; border-radius: 8px;
            padding: 14px; font-size: 0.92rem; font-weight: 600; font-family: 'Segoe UI', sans-serif;
            cursor: pointer; margin-top: 22px; display: flex; justify-content: center;
            align-items: center; gap: 8px; transition: background 0.2s;
        }
        .btn-pay:hover { background: #1d5c30; }
        .trust-row { display: flex; justify-content: center; gap: 22px; margin-top: 14px; }
        .trust-item { font-size: 0.72rem; color: #aaa; font-family: 'Segoe UI', sans-serif; display: flex; align-items: center; gap: 5px; }
        .trust-item i { color: #2d7a45; }
        .summary-title { font-size: 0.95rem; font-weight: 700; margin-bottom: 20px; }
        .order-item-row { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
        .item-icon { width: 46px; height: 46px; border-radius: 8px; background: #f0f7f3; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; flex-shrink: 0; }
        .item-details { flex: 1; }
        .item-name { font-size: 0.85rem; font-weight: 600; line-height: 1.3; }
        .item-sub { font-size: 0.75rem; color: #999; font-family: 'Segoe UI', sans-serif; }
        .item-price { font-size: 0.88rem; font-weight: 600; color: #1a1a1a; white-space: nowrap; }
        .divider { border-top: 1px solid #eee; margin: 14px 0; }
        .totals-row { display: flex; justify-content: space-between; font-size: 0.84rem; font-family: 'Segoe UI', sans-serif; color: #777; margin-bottom: 6px; }
        .totals-final { display: flex; justify-content: space-between; font-size: 0.95rem; font-weight: 700; color: #1a3c2d; margin-top: 8px; }
        .freshness-box { background: #f0f7f3; border-radius: 8px; padding: 12px 14px; margin-top: 16px; display: flex; gap: 10px; }
        .freshness-box i { color: #2d7a45; flex-shrink: 0; margin-top: 2px; }
        .freshness-text { font-size: 0.76rem; font-family: 'Segoe UI', sans-serif; color: #666; line-height: 1.5; }
        .freshness-text b { color: #1a3c2d; display: block; margin-bottom: 2px; }
        footer { border-top: 1px solid #ddd; padding: 18px 32px; display: flex; justify-content: space-between; font-family: 'Segoe UI', sans-serif; font-size: 0.75rem; color: #aaa; }
        footer a { color: #aaa; text-decoration: none; margin-left: 16px; }
        footer a:hover { color: #1a3c2d; }
        @media (max-width: 700px) {
            .main-grid { grid-template-columns: 1fr; padding: 0 16px 40px; }
            .page-heading { padding: 24px 16px 14px; }
        }
    </style>
</head>
<body>

<!-- Navbar -->
<nav class="top-nav">
    <span class="brand-name">EgertonDairySales</span>
    <div class="nav-links">
        <a href="${pageContext.request.contextPath}/orders.jsp">My Orders</a>
        <a href="#">Support</a>
    </div>
</nav>

<!-- Page heading -->
<div class="page-heading">
    <h1>Secure Checkout</h1>
    <p>Complete your order.</p>
</div>

<!-- Main content -->
<div class="main-grid">

    <!-- LEFT: M-Pesa payment form -->
    <div>

        <%-- Error message — displayed using JSTL <c:if> instead of Java scriptlet if-block --%>
        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger mb-3" style="font-family:'Segoe UI',sans-serif; font-size:0.85rem; border-radius:8px;">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>${errorMessage}
            </div>
        </c:if>

        <div class="card-box">
            <div class="mpesa-row">
                <span class="mpesa-label">
                    <i class="bi bi-phone-fill"></i> M-Pesa Payment
                </span>
                <span class="mpesa-pill">M-PESA ●</span>
            </div>

            <%-- Form uses EL ${payment.orderId} and ${payment.amount}
                 instead of scriptlet <%= orderId %> expressions --%>
            <form action="${pageContext.request.contextPath}/pay" method="post">
                <input type="hidden" name="orderId" value="${payment.orderId}">
                <input type="hidden" name="amount"  value="${payment.amount}">

                <div class="field-label">M-Pesa Phone Number</div>
                <input
                    type="tel"
                    class="phone-field"
                    name="phone"
                    placeholder="+254 722 000 000"
                    required
                    pattern="^(0|\+?254)?[7][0-9]{8}$"
                    title="Enter a valid Safaricom number">
                <p class="phone-hint">
                    Enter the Safaricom number you wish to pay with. You will receive
                    an STK push prompt on your handset to enter your M-Pesa PIN.
                </p>

                <button type="submit" class="btn-pay">
                    Pay Now with Lipa Na M-Pesa &nbsp;→
                </button>
            </form>

            <div class="trust-row">
                <span class="trust-item"><i class="bi bi-shield-lock-fill"></i> SECURE SSL</span>
                <span class="trust-item"><i class="bi bi-patch-check-fill"></i> M-PESA VERIFIED</span>
            </div>
        </div>

        <div class="mt-3" style="font-family:'Segoe UI',sans-serif; font-size:0.82rem;">
            <a href="${pageContext.request.contextPath}/checkout.jsp" style="color:#aaa; text-decoration:none;">
                <i class="bi bi-arrow-left"></i> Back to Checkout
            </a>
        </div>
    </div>

    <!-- RIGHT: Order summary -->
    <div class="card-box" style="align-self: start;">
        <p class="summary-title">Order Summary</p>

        <div class="order-item-row">
            <div class="item-icon">🥛</div>
            <div class="item-details">
                <div class="item-name">Dairy Products</div>
                <%-- EL reads orderId directly from the Payment bean --%>
                <div class="item-sub">Order #${payment.orderId}</div>
            </div>
            <div class="item-price">
                KES <fmt:formatNumber value="${subtotal}" type="number" minFractionDigits="0" maxFractionDigits="0"/>
            </div>
        </div>

        <div class="divider"></div>

        <div class="totals-row">
            <span>Subtotal</span>
            <%-- fmt:formatNumber replaces String.format("%,.2f", subtotal) --%>
            <span>KES <fmt:formatNumber value="${subtotal}" type="number" minFractionDigits="2"/></span>
        </div>
        <div class="totals-row">
            <span>Delivery Fee</span>
            <span>KES <fmt:formatNumber value="${deliveryFee}" type="number" minFractionDigits="2"/></span>
        </div>

        <div class="divider"></div>

        <div class="totals-final">
            <span>Total</span>
            <%-- EL reads amount from the Payment bean's getAmount() method --%>
            <span>KES <fmt:formatNumber value="${payment.amount}" type="number" minFractionDigits="2"/></span>
        </div>

        <div class="freshness-box">
            <i class="bi bi-patch-check-fill"></i>
            <div class="freshness-text">
                <b>Guaranteed Freshness</b>
                Direct from Egerton University farms. Harvested and delivered within 24 hours of your order.
            </div>
        </div>
    </div>

</div>

<!-- Footer -->
<footer>
    <span>EgertonDairySales &copy; 2025 Egerton University Dairy.</span>
    <div>
        <a href="#">Privacy Policy</a>
        <a href="#">Terms of Service</a>
        <a href="#">Shipping Info</a>
    </div>
</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

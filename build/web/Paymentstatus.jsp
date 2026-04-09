<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%-- ══════════════════════════════════════════════════════════
     Java Bean declaration — loads Payment bean from SESSION scope.
     The PaymentServlet stored payment details in session after
     the M-Pesa STK push. EL reads from this bean: ${payment.xxx}
     ══════════════════════════════════════════════════════════ --%>
<jsp:useBean id="payment" class="servlet.model.Payment" scope="session" />

<%-- Set bean properties from session attributes using jsp:setProperty --%>
<jsp:setProperty name="payment" property="paymentStatus"   value="${sessionScope.paymentStatus}" />
<jsp:setProperty name="payment" property="transactionCode" value="${sessionScope.transactionCode}" />

<%-- JSTL tag libraries --%>
<%@ taglib prefix="c"   uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- Convenience variables set with <c:set> — replaces Java boolean variables --%>
<c:set var="orderId"     value="${sessionScope.paymentOrderId != null ? sessionScope.paymentOrderId : 0}" />
<c:set var="orderRef"    value="EDS-${10000 + orderId}" />
<c:set var="isCompleted" value="${payment.paymentStatus eq 'Completed'}" />
<c:set var="isFailed"    value="${payment.paymentStatus eq 'Failed' or payment.paymentStatus eq 'Cancelled'}" />
<c:set var="isPending"   value="${payment.paymentStatus eq 'Pending' or empty payment.paymentStatus}" />

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <%-- Page title uses JSTL <c:choose> instead of Java ternary --%>
    <title>
        <c:choose>
            <c:when test="${isCompleted}">Order Confirmed</c:when>
            <c:when test="${isFailed}">Payment Failed</c:when>
            <c:otherwise>Payment Pending</c:otherwise>
        </c:choose>
        | Dairy Sales System
    </title>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/font/bootstrap-icons.css" rel="stylesheet">

    <%-- Auto-refresh while pending — JSTL <c:if> replaces Java if block --%>
    <c:if test="${isPending}">
        <meta http-equiv="refresh" content="8">
    </c:if>

    <style>
        body { background-color: #f2f2ec; font-family: Georgia, serif; color: #1a1a1a; margin: 0; }
        .top-nav { background: #f2f2ec; border-bottom: 1px solid #ddd; padding: 14px 32px; display: flex; justify-content: space-between; align-items: center; }
        .brand-name { font-size: 1rem; font-weight: 700; color: #1a3c2d; }
        .nav-status { font-size: 0.72rem; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase; font-family: 'Segoe UI', sans-serif; }
        .status-confirmed { color: #2d7a45; }
        .status-pending   { color: #b07d00; }
        .status-failed    { color: #c0392b; }
        .page-container { max-width: 860px; margin: 40px auto; padding: 0 24px 60px; }
        .icon-circle { width: 72px; height: 72px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.8rem; margin: 0 auto 20px; }
        .icon-success { background: #2d7a45; color: white; }
        .icon-pending { background: #f0c040; color: #7a5200; }
        .icon-failed  { background: #c0392b; color: white; }
        .status-heading { text-align: center; margin-bottom: 6px; }
        .status-heading h2 { font-size: 1.7rem; font-weight: 700; color: #1a3c2d; }
        .status-heading p { color: #888; font-size: 0.85rem; font-family: 'Segoe UI', sans-serif; font-style: italic; }
        .cards-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 30px; }
        .card-box { background: #fff; border-radius: 12px; padding: 24px; border: 1px solid #e0e0da; }
        .card-section-title { font-size: 0.78rem; font-weight: 700; color: #2d7a45; font-family: 'Segoe UI', sans-serif; letter-spacing: 0.3px; margin-bottom: 16px; }
        .info-label { font-size: 0.65rem; text-transform: uppercase; letter-spacing: 1px; color: #aaa; font-family: 'Segoe UI', sans-serif; margin-bottom: 3px; }
        .info-value { font-size: 0.88rem; font-family: 'Segoe UI', sans-serif; color: #1a1a1a; margin-bottom: 14px; display: flex; align-items: center; gap: 7px; }
        .info-value i { color: #2d7a45; font-size: 0.85rem; }
        .btn-track { width: 100%; background: #2d7a45; color: white; border: none; border-radius: 8px; padding: 12px; font-size: 0.88rem; font-weight: 600; font-family: 'Segoe UI', sans-serif; display: flex; align-items: center; justify-content: center; gap: 7px; text-decoration: none; margin-bottom: 10px; transition: background 0.2s; }
        .btn-track:hover { background: #1d5c30; color: white; }
        .btn-home { width: 100%; background: #f5f5ef; color: #555; border: 1px solid #ddd; border-radius: 8px; padding: 11px; font-size: 0.85rem; font-family: 'Segoe UI', sans-serif; display: flex; align-items: center; justify-content: center; gap: 7px; text-decoration: none; transition: background 0.2s; }
        .btn-home:hover { background: #ebebeb; color: #1a1a1a; }
        .order-item-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
        .item-icon { width: 42px; height: 42px; border-radius: 7px; background: #f0f7f3; display: flex; align-items: center; justify-content: center; font-size: 1.1rem; flex-shrink: 0; }
        .item-name { font-size: 0.84rem; font-weight: 600; line-height: 1.3; flex: 1; }
        .item-sub { font-size: 0.74rem; color: #aaa; font-family: 'Segoe UI', sans-serif; }
        .item-price { font-size: 0.84rem; font-weight: 600; color: #1a3c2d; white-space: nowrap; }
        .divider { border-top: 1px solid #eee; margin: 12px 0; }
        .totals-row { display: flex; justify-content: space-between; font-size: 0.82rem; font-family: 'Segoe UI', sans-serif; color: #888; margin-bottom: 5px; }
        .totals-final { display: flex; justify-content: space-between; font-size: 0.9rem; font-weight: 700; color: #1a3c2d; margin-top: 8px; }
        .full-status-card { background: #fff; border-radius: 12px; padding: 48px 32px; text-align: center; border: 1px solid #e0e0da; margin-top: 30px; }
        .pending-spinner { width: 48px; height: 48px; border: 4px solid #f0c040; border-top-color: transparent; border-radius: 50%; animation: spin 0.9s linear infinite; margin: 20px auto; }
        @keyframes spin { to { transform: rotate(360deg); } }
        .support-line { text-align: center; margin-top: 28px; font-size: 0.82rem; font-family: 'Segoe UI', sans-serif; color: #aaa; }
        .support-line a { color: #2d7a45; text-decoration: none; font-weight: 600; }
        footer { border-top: 1px solid #ddd; padding: 18px 32px; display: flex; justify-content: space-between; font-family: 'Segoe UI', sans-serif; font-size: 0.75rem; color: #aaa; }
        footer a { color: #aaa; text-decoration: none; margin-left: 16px; }
        footer a:hover { color: #1a3c2d; }
        @media (max-width: 620px) { .cards-grid { grid-template-columns: 1fr; } .page-container { padding: 0 14px 40px; } }
    </style>
</head>
<body>

<!-- Navbar — <c:choose> replaces Java if/else if/else chain -->
<nav class="top-nav">
    <span class="brand-name">EgertonDairySales</span>
    <c:choose>
        <c:when test="${isCompleted}">
            <span class="nav-status status-confirmed">ORDER CONFIRMED</span>
        </c:when>
        <c:when test="${isFailed}">
            <span class="nav-status status-failed">PAYMENT FAILED</span>
        </c:when>
        <c:otherwise>
            <span class="nav-status status-pending">PAYMENT PENDING</span>
        </c:otherwise>
    </c:choose>
</nav>

<div class="page-container">

    <c:choose>

        <%-- ════════════ SUCCESS STATE ════════════ --%>
        <c:when test="${isCompleted}">

            <div style="text-align:center; margin-bottom:4px;">
                <div class="icon-circle icon-success" style="margin-top:12px;">
                    <i class="bi bi-check-lg"></i>
                </div>
                <div class="status-heading">
                    <h2>Order Placed Successfully!</h2>
                    <p>Thank you for supporting modern pastoral excellence.</p>
                </div>
            </div>

            <div class="cards-grid">

                <!-- Delivery Info card -->
                <div class="card-box">
                    <div class="card-section-title">Delivery Information</div>

                    <div class="info-label">Order Number</div>
                    <%-- EL reads orderRef set by <c:set> above --%>
                    <div class="info-value">#${orderRef}</div>

                    <div class="info-label">Estimated Delivery</div>
                    <div class="info-value">
                        <i class="bi bi-clock"></i> Today, 4:00 PM – 5:00 PM
                    </div>

                    <div class="info-label">Shipping To</div>
                    <div class="info-value">Main Campus, Egerton University</div>

                    <%-- Only show receipt row if value exists — <c:if> replaces Java null check --%>
                    <c:if test="${not empty payment.transactionCode}">
                        <div class="info-label">M-Pesa Receipt</div>
                        <%-- EL reads transactionCode from Payment bean's getTransactionCode() --%>
                        <div class="info-value" style="font-size:0.8rem; color:#555;">
                            ${payment.transactionCode}
                        </div>
                    </c:if>

                    <a href="${pageContext.request.contextPath}/orders.jsp" class="btn-track">
                        <i class="bi bi-truck"></i> Track My Order
                    </a>
                    <a href="${pageContext.request.contextPath}/products.jsp" class="btn-home">
                        <i class="bi bi-house"></i> Back to Home
                    </a>
                </div>

                <!-- Order Summary card -->
                <div class="card-box">
                    <div class="card-section-title">Order Summary</div>

                    <%-- Static sample — replace with DB loop when team integrates --%>
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
                        <span>Subtotal</span><span>KES 1,290</span>
                    </div>
                    <div class="totals-row">
                        <span>Delivery Fee</span><span>KES 150</span>
                    </div>

                    <div class="divider"></div>

                    <div class="totals-final">
                        <span>Total Paid</span>
                        <%-- fmt:formatNumber replaces String.format() --%>
                        <span>KES <fmt:formatNumber value="${payment.amount}" type="number" minFractionDigits="2"/></span>
                    </div>
                </div>

            </div>

            <div class="support-line">
                Need help with your order? <a href="#">Contact Support</a>
            </div>

        </c:when>

        <%-- ════════════ PENDING STATE ════════════ --%>
        <c:when test="${isPending}">
            <div class="full-status-card">
                <div class="icon-circle icon-pending" style="margin:0 auto 16px;">
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
                <c:if test="${not empty payment.transactionCode}">
                    <p style="font-family:'Segoe UI',sans-serif; font-size:0.72rem; color:#ccc; margin-top:8px;">
                        Ref: ${payment.transactionCode}
                    </p>
                </c:if>
                <a href="${pageContext.request.contextPath}/products.jsp"
                   class="btn-home" style="max-width:220px; margin:16px auto 0;">
                    Cancel & Go Back
                </a>
            </div>
        </c:when>

        <%-- ════════════ FAILED STATE ════════════ --%>
        <c:otherwise>
            <div class="full-status-card">
                <div class="icon-circle icon-failed" style="margin:0 auto 16px;">
                    <i class="bi bi-x-lg"></i>
                </div>
                <h4 style="font-weight:700; color:#c0392b;">Payment Failed</h4>
                <p style="font-family:'Segoe UI',sans-serif; color:#888; font-size:0.88rem;">
                    The payment was not completed. You may have cancelled it or it timed out.
                </p>
                <div style="display:flex; gap:12px; justify-content:center; margin-top:20px; flex-wrap:wrap;">
                    <%-- EL builds the retry URL using orderId from session --%>
                    <a href="${pageContext.request.contextPath}/pay?orderId=${orderId}"
                       class="btn-track" style="max-width:180px;">
                        <i class="bi bi-arrow-repeat"></i> Try Again
                    </a>
                    <a href="${pageContext.request.contextPath}/products.jsp"
                       class="btn-home" style="max-width:180px;">
                        <i class="bi bi-house"></i> Back to Shop
                    </a>
                </div>
            </div>
        </c:otherwise>

    </c:choose>

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

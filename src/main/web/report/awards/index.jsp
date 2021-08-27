<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<!DOCTYPE HTML>
<html>
<head>
<title>Award Configuration</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>Configuration of how the awards report and awards script are
        generated.</p>

    <a class="wide"
        href="<c:url value='/report/awards/edit-categories-awarded.jsp'/>">Specify
        the categories that are awarded at each tournament level.</a>

</body>
</html>

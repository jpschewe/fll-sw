<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.playoff.Sponsors.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Sponsors</title>
<script type="text/javascript">
  window.setInterval("location.href='sponsors.jsp'", 15000);
</script>
</head>
<body>
    <c:if test="${numLogos > 0}">
        <table class="center">
            <tr>
                <td width="50%"
                    style="text-align: right; vertical-align: middle">
                    This season sponsored by:</td>
                <td width="50%"
                    style="text-align: left; vertical-align: middle">
                    <img src='<c:url value="/${imageFileName}"/>' />
                </td>
            </tr>
        </table>
    </c:if>
</body>
</html>

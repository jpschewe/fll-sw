<%@ include file="/WEB-INF/jspf/init.jspf"%>
<fll-sw:required-roles roles="PUBLIC" allowSetup="true" />

<%
fll.web.ChallengeDescriptions.populateContext(pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<body>
    <ul>
        <c:forEach items="${descriptions}" var="description">
            <li>${description.display}
                <a
                    href="<c:url value='/challenge.xml?url=${description.URL}' />"
                    target="_blank">View</a> <a
                    href="<c:url value='/challenge.xml?download=true&url=${description.URL}' />">Download</a>
            </li>
        </c:forEach>

    </ul>
</body>
</html>

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
    <p>Use this page to view the known challenge descriptions. This
        can be helpful when determining which one to choose for setup.</p>
    <p>If you want to modify one, you can download it from here and
        edit it in the Challenge Editor. The Challenge Editor is
        accessible as a button on the main window that appeared when the
        application started up. Once the challenge description is
        modified, it can be uploaded through the setup page.</p>
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

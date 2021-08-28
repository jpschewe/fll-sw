<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.report.awards.AwardsIndex.populateContext(application, pageContext);
%>

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

    <h1>Configuration of how the awards report and awards script
        are generated</h1>

    <a class="wide"
        href="<c:url value='/report/awards/edit-categories-awarded.jsp'/>">Specify
        the categories that are awarded at each tournament level.</a>

    <h2>Awards Script</h2>
    <a class="wide"
        href="<c:url value='/report/awards/edit-awards-script.jsp'/>">Edit
        the awards script for the season. This will effect ALL
        tournaments.</a>

    <h3>Tournament Levels</h3>
    <div>Changes here will effect all tournaments at the selected
        level.</div>
    <c:forEach items="${tournamentLevels}" var="level">

        <a class="wide"
            href="<c:url value='/report/awards/edit-awards-script.jsp?level=${level.id}'/>">Edit
            the awards script for all ${level.name} tournaments.</a>

    </c:forEach>

    <h3>Tournaments</h3>
    <div>Changes here will only effect the selected tournament.</div>
    <c:forEach items="${tournaments}" var="tournament">
        <a class="wide"
            href="<c:url value='/report/awards/edit-awards-script.jsp?tournament=${tournament.tournamentID}'/>">Edit
            the awards script for the ${tournament.name} tournament.</a>
    </c:forEach>

</body>
</html>

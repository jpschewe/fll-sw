<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="JUDGE,REF" allowSetup="false" />

<%
fll.web.report.TopScoreReport.populateContextPerAwardGroup(application, pageContext);
%>


<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Top regular match play round scores</title>
</head>
<body>

    <h1>Top regular match play round scores</h1>

    <c:forEach var="entry" items="${scoreMap}">
        <h2>
            Award Group
            <c:out value="${entry.key}" />
        </h2>

        <table border='1'>
            <tr>
                <th>Rank</th>
                <th>Team Number</th>
                <th>Team Name</th>
                <th>Organization</th>
                <th>Score</th>
            </tr>
            <c:forEach var="score" items="${entry.value}">
                <tr>
                    <td>${score.rank}</td>
                    <td>${score.teamNumber}
                    <td>${score.teamName}</td>
                    <td>${score.organization}</td>
                    <td>${score.formattedScore}</td>
                </tr>
            </c:forEach>
        </table>
    </c:forEach>
</body>
</html>

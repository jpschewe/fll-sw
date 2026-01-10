<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles
    roles="REF,HEAD_JUDGE,REPORT_GENERATOR,SCORING_COORDINATOR"
    allowSetup="false" />

<%
fll.web.report.UnverifiedRuns.populateContext(application, session, request, pageContext);
%>

<html>
<head>
<title>Unverified Runs</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>

    <h1>Unverified Runs</h1>
    <hr />
    <h2>
        Tournament:
        <c:out value="${tournamentData.currentTournament.name}" />
    </h2>
    <table border='1'>
        <tr>
            <th>Team #</th>
            <th>Team Name</th>
            <th>Run</th>
            <th>Table</th>
            <th>Edit</th>
        </tr>

        <c:forEach var="score" items="${data}">
            <tr>
                <td>${score.teamNumber}</td>
                <td>${score.teamName}</td>
                <td>${score.runName}</td>
                <td>${score.tablename}</td>
                <td>
                    <a
                        href='<c:url value="/scoreEntry/scoreEntry.jsp?TeamNumber=${score.teamNumber}&EditFlag=true&RunNumber=${score.runNumber}"/>'>Edit
                    </a>
                </td>
            </tr>
        </c:forEach>
    </table>

</body>
</html>

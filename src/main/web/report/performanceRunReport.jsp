<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.ApplicationAttributes"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>

<fll-sw:required-roles roles="REF,JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.web.report.PerformanceRunReport.populateContext(application, session, request, pageContext);
final DataSource datasource = ApplicationAttributes.getDataSource(application);
final Connection connection = datasource.getConnection();
pageContext.setAttribute("tournament", Queries.getCurrentTournament(connection));
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Performance Run ${RunNumber}</title>

<style>
table#perf-data {
    border-collapse: collapse;
}

table#perf-data, table#perf-data th, table#perf-data td {
    border: 1px solid black;
}

table#perf-data th, table#perf-data td {
    padding-right: 5px;
    padding-left: 5px;
}
</style>

</head>

<body>
    <h1>Performance Run ${RunNumber}</h1>

    <c:forEach var="division" items="${divisions}">
        <h2>
            Award Group
            <c:out value="${division}" />
        </h2>
        <table id="perf-data">
            <tr>
                <th>Team Number</th>
                <th>Team Name</th>
                <th>Organization</th>
                <th>Score</th>
                <th>Last Edited</th>
                <th></th>
            </tr>
            <sql:query var="result" dataSource="${datasource}">
            SELECT Teams.TeamNumber,Teams.TeamName,Teams.Organization,Performance.ComputedTotal,Performance.NoShow,Performance.TIMESTAMP
                     FROM Teams,Performance,TournamentTeams
                     WHERE Performance.RunNumber = <c:out
                    value="${RunNumber}" />
                       AND Teams.TeamNumber = Performance.TeamNumber
                       AND TournamentTeams.TeamNumber = Teams.TeamNumber
                       AND Performance.Tournament = <c:out
                    value="${tournament}" />
                       AND TournamentTeams.event_division  = '<c:out
                    value="${division}" />'
                       AND TournamentTeams.Tournament = Performance.Tournament
                       ORDER BY ComputedTotal DESC
          </sql:query>
            <c:forEach items="${result.rows}" var="row">
                <tr>
                    <td>${row.TeamNumber}</td>
                    <td>${row.TeamName}</td>
                    <td>${row.Organization}</td>
                    <td>
                        <c:choose>
                            <c:when test="${row.NoShow == True}">
                            No Show
                            </c:when>
                            <c:otherwise>
                                    ${row.ComputedTotal}
                                </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="right">
                        <fmt:formatDate value="${row.TIMESTAMP}"
                            pattern="h:mm" />
                    </td>
                    <td>
                        <a
                            href='<c:url value="/scoreEntry/scoreEntry.jsp?TeamNumber=${row.TeamNumber}&EditFlag=true&RunNumber=${param.RunNumber}&workflow_id=${workflow_id}"/>'>Edit
                        </a>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </c:forEach>


</body>
</html>

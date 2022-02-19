<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.ApplicationAttributes"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>

<fll-sw:required-roles roles="REF,JUDGE" allowSetup="false" />

<%
final DataSource datasource = ApplicationAttributes.getDataSource(application);
final Connection connection = datasource.getConnection();
pageContext.setAttribute("tournament", Queries.getCurrentTournament(connection));
pageContext.setAttribute("divisions", Queries.getAwardGroups(connection));
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Performance Run
    <c:out value="${param.RunNumber}" /></title>

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
    <h1>
        Performance Run
        <c:out value="${param.RunNumber}" />
    </h1>

    <c:choose>
        <c:when test="${empty param.RunNumber}">
            <font color='red'>You must specify a run number!</font>
        </c:when>
        <c:otherwise>
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
                    </tr>
                    <sql:query var="result" dataSource="${datasource}">
            SELECT Teams.TeamNumber,Teams.TeamName,Teams.Organization,Performance.ComputedTotal,Performance.NoShow,Performance.TIMESTAMP
                     FROM Teams,Performance,TournamentTeams
                     WHERE Performance.RunNumber = <c:out
                            value="${param.RunNumber}" />
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
                        </tr>
                    </c:forEach>
                </table>
            </c:forEach>
        </c:otherwise>
    </c:choose>


</body>
</html>

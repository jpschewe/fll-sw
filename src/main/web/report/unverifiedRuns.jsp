<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="javax.sql.DataSource"%>

<fll-sw:required-roles roles="REF,HEAD_JUDGE" allowSetup="false" />

<%
final DataSource datasource = ApplicationAttributes.getDataSource(application);
final Connection connection = datasource.getConnection();
pageContext.setAttribute("currentTournament", Queries.getCurrentTournament(connection));
%>

<html>
<head>
<title>Unverified Runs</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>

    <h1>FLL Unverified Runs</h1>
    <hr />
    <h2>
        Tournament:
        <c:out value="${currentTournament}" />
    </h2>
    <table border='0'>
        <tr>
            <th>Team #</th>
            <th>Run Number</th>
        </tr>
        <sql:query var="result" dataSource="${datasource}">
   SELECT
     TeamNumber
    ,RunNumber
     FROM Performance
     WHERE Verified <> TRUE 
       AND Tournament = <c:out value="${currentTournament}" />
       ORDER BY RunNumber
 </sql:query>
        <c:forEach var="row" items="${result.rowsByIndex}">
            <tr>
                <td>
                    <c:out value="${row[0]}" />
                </td>
                <td>
                    <c:out value="${row[1]}" />
                </td>
            </tr>
        </c:forEach>
    </table>

</body>
</html>

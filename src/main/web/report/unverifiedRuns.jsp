<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="javax.sql.DataSource"%>

<fll-sw:required-roles roles="REF,HEAD_JUDGE,REPORT_GENERATOR" allowSetup="false" />

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
    <table border='1'>
        <tr>
            <th>Team #</th>
            <th>Run Number</th>
            <th>Table</th>
            <th>Edit</th>
        </tr>
        <sql:query var="result" dataSource="${datasource}">
   SELECT
     TeamNumber
    ,RunNumber
    ,tablename
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
                <td>
                    <c:out value="${row[2]}" />
                </td>
                <td>
                    <a
                        href='<c:url value="/scoreEntry/scoreEntry.jsp?TeamNumber=${row[0]}&EditFlag=true&RunNumber=${row[1]}"/>'>Edit
                    </a>
                </td>
            </tr>
        </c:forEach>
    </table>

</body>
</html>

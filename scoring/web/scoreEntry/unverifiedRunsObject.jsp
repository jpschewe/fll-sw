<%@ page contentType="text/javascript" %>

<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="fll.db.Queries"%>

<%@ page import="java.util.Collection" %>

<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions" %>

<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
      pageContext.setAttribute("currentTournament", Queries.getCurrentTournament(connection));
%>


<sql:query var="result" dataSource="${datasource}">
   SELECT
     Performance.TeamNumber
    ,Performance.RunNumber
    ,Teams.TeamName
     FROM Performance, Teams
     WHERE Verified != TRUE 
       AND Tournament = ${currentTournament}
       AND Teams.TeamNumber = Performance.TeamNumber
       ORDER BY Performance.RunNumber, Teams.TeamNumber
 </sql:query>
 <% int index = 0; %>
              <c:forEach var="row" items="${result.rowsByIndex}">
                
                document.verify.TeamNumber.options[<%= index %>]=new Option("Run ${row[1]} - ${row[0]}    [${row[2]}]", "${row[0]}-${row[1]}", true, false);
                <% index = index + 1;  %>
              </c:forEach>
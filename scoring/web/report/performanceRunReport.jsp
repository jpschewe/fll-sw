<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.Queries" %>
  
<%@ page import="java.text.NumberFormat" %>
  
<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

final String runNumberStr = request.getParameter("RunNumber");
if(null == runNumberStr) {
  throw new RuntimeException("You must specify a run number!");
}
final int runNumber = NumberFormat.getNumberInstance().parse(runNumberStr).intValue();
  
  
final Connection connection = (Connection)application.getAttribute("connection");
final String tournament = Queries.getCurrentTournament(connection);
final Statement stmt = connection.createStatement();
final Iterator divisionIter = Queries.getDivisions(connection).iterator();
%>
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Performance Run <%=runNumber%>)</title>
  </head>

  <body background="<c:url value="/images/bricks1.gif" />" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Performance Run <%=runNumber%>)</h1>

<%
  while(divisionIter.hasNext()) {
    final String division = (String)divisionIter.next();
%>
      <h2>Division <%=division%></h2>
      <table border='1'>
        <tr>
         <th>Team Number </th>
         <th>Team Name </th>
         <th>Score</th>
       </tr>
<%
    ResultSet rs = stmt.executeQuery("SELECT Teams.TeamNumber,Teams.TeamName,Performance.ComputedTotal FROM Teams,Performance WHERE Performance.RunNumber = " + runNumber + " AND Teams.TeamNumber = Performance.TeamNumber AND Performance.Tournament = '" + tournament + "' AND Teams.Division  = " + division + " ORDER BY ComputedTotal DESC");
    while(rs.next()) {
      final int teamNumber = rs.getInt(1);
      final String teamName = rs.getString(2);
      final double score = rs.getDouble(3);
%>
     <tr>
       <td><%=teamNumber%></td>
       <td><%=teamName%></td>
       <td><%=score%></td>
     </tr>

<%
    }
    Utilities.closeResultSet(rs);
%>
      </table>
<%
  }
%>
<%
  Utilities.closeStatement(stmt);
%>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

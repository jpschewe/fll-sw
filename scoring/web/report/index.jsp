<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.Utilities" %>
  
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Connection" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

final Connection connection = (Connection)application.getAttribute("connection");
final Statement stmt = connection.createStatement();
final ResultSet rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance");
final int maxRunNumber;
if(rs.next()) {
  maxRunNumber = rs.getInt(1);
} else {
  maxRunNumber = 1;
}
Utilities.closeResultSet(rs);
Utilities.closeStatement(stmt);
%>
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Reporting)</title>
  </head>

  <body>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Reporting)</h1>

    <ol>
      <li><a href="summarizePhase1.jsp">Compute summarized scores</a>.  This
      needs to be executed before any reports can be generated.  You will be
      returned to this page if there are no errors summarizing scores.</li>

      <li><a href="finalComputedScores.jsp">Final Computed Scores</a></li>
        
      <li><a href="categorizedScores.jsp">Categorized Scores</a></li>

      <li><a href="scoreGroupScores.jsp">Categorized Scores by score group</a></li>
        
      <li>
          <form ACTION='performanceRunReport.jsp' METHOD='POST'>
          Show reports for performance run <select name='RunNumber'>
<% for(int i=0; i<maxRunNumber; i++) { %>
  <option value='<%=(i+1)%>'><%=(i+1)%></option>
<% } %>
          </select>
          <input type='submit' value='Generate report'>
          </form>
       </li>
    </ol>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

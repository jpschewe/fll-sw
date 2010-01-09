<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.ScoreStandardization" %>
<%@ page import="fll.db.Queries" %>
<%@ page import="fll.Utilities" %>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.web.SessionAttributes" %>
      
<%@ page import="org.w3c.dom.Document" %>

<%@ page import="java.util.Map" %>

<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>


<%
final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
final int currentTournament = Queries.getCurrentTournament(connection);
  
Queries.updateScoreTotals(challengeDocument, connection);
      
ScoreStandardization.standardizeSubjectiveScores(connection, challengeDocument, currentTournament);
ScoreStandardization.summarizeScores(connection, challengeDocument, currentTournament);

%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Summarize Scores</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Summarize Scores)</h1>

    <p>Below is a list of judges found and what categories they scored.</p>
          
    <table border='1'>
      <tr>
        <th>Judge</th>
        <th>Category</th>
        <td>Num Teams Scored</th>
      </tr>
      <x:forEach select="$challengeDocument/fll/subjectiveCategory">
        <sql:query var="result" dataSource="${datasource}">
          SELECT Judge, COUNT(ComputedTotal) AS numTeams FROM <x:out select="./@name"/> WHERE Tournament = <%=currentTournament%> AND NoShow = false AND ComputedTotal IS NOT NULL GROUP BY Judge
        </sql:query>
        <c:forEach items="${result.rows}" var="row">
        <tr>
          <td><c:out value="${row.Judge}"/></td>
          <td><x:out select="./@title"/></td>
          <td><c:out value="${row.numTeams}"/></td>
        <tr>
        </c:forEach>
      </x:forEach>
    </table>
        
    <p>If these look correct, <a href="summarizePhase2.jsp">continue</a> on to
    the second phase of computing the scores.  This page will return you to
    the reporting menu if everything is ok.</p>
        

  </body>
</html>

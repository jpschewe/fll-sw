<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.ScoreStandardization" %>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>

<%
final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
final DataSource datasource = ApplicationAttributes.getDataSource();
final Connection connection = datasource.getConnection();
final int currentTournament = Queries.getCurrentTournament(connection);
  
ScoreStandardization.updateTeamTotalScores(connection, challengeDocument, currentTournament);
final String errorMsg = ScoreStandardization.checkDataConsistency(connection);

%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Summarize Scores</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Summarize Scores)</h1>

<%if(null == errorMsg) {%>
  <a href="index.jsp">Normally you'd be redirected here</a>
  <%
  session.setAttribute(SessionAttributes.MESSAGE, "<p id='success'><i>Successfully summarized scores</i></p>");
  response.sendRedirect(response.encodeRedirectURL("index.jsp")); 
  %>
<%} else {%>
<p><font color='red'><%=errorMsg%></font></p>
<%}%>


  </body>
</html>

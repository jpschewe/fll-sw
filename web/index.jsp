<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.sql.Connection" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
%>

<c:if test="${not empty param.ScorePageText}">
  <c:set var="ScorePageText" value="${param.ScorePageText}" scope="application"/>
</c:if>

<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/></title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/></h1>      
    <ul>

      <li>Current Tournament -> <%=Queries.getCurrentTournament(connection)%></li>
      
      <li><a href="instructions.jsp">Instructions</a></li>
        
      <li><a href="scoreEntry/select_team.jsp">Score Entry</a></li>
        
      <li><a href='scoreboard/index.jsp'>Scoreboard</a></li>
        
      <li><a href="playoff/index.jsp">Playoffs</a></li>
        
      <li><a href="report/index.jsp">Tournament reporting</a></li>
        
      <li><a href="admin/index.jsp">Administration</a></li>

      <li><a href='display.jsp'>Big Screen Display</a>  Follow this link on the computer that's used to display scores with the projector.</li>
              
      <li>
        <form action='index.jsp' method='post'>
          Score page text: 
          <input type='text' name='ScorePageText' value='<c:out value="${ScorePageText}"/>'>
          <input type='submit' value='Change text'>
        </form>
      </li>

      <li><a href="developer/index.jsp">Developer page</a></li>
      
      <li><a href="troubleshooting/index.jsp">Troubleshooting</a></li>
        
      <li><a href="credits/credits.jsp">Credits</a></li>
        
    </ul>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

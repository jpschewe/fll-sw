<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="org.w3c.dom.Document" %>
  
<%@ page import="java.sql.Connection" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

if(null != request.getParameter("ScorePageText")) {
  application.setAttribute("ScorePageText", request.getParameter("ScorePageText"));
}

final Connection connection = (Connection)application.getAttribute("connection");
%>
      
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%></title>
  </head>

  <body background="images/bricks1.gif" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%></h1>

    <ul>
        
      <li>Current Tournament -> <%=Queries.getCurrentTournament(connection)%></li>
      
      <li><a href="instructions.jsp">Instructions</a></li>
        
      <li><a href="scoreEntry/select_team.jsp">Score Entry</a></li>
        
      <li><a href='scoreboard/index.jsp'>Scoreboard</a></li>
        
      <li><a href="playoff/index.jsp">Playoffs</a></li>
        
      <li><a href="report/index.jsp">Tournament reporting</a></li>
        
      <li><a href="admin/index.jsp">Administration</a></li>
        
      <li>
        <form action='index.jsp' method='post'>
          Score page text: 
          <input type='text' name='ScorePageText' value='<%=application.getAttribute("ScorePageText")%>'>
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

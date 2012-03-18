<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="fll.web.WebUtils"%>

<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
pageContext.setAttribute("urls", WebUtils.getAllURLs(request));
%>


<html>
  <head>
    <title>FLL-SW</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/></h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <ul>

      <li>Current Tournament -&gt; <%=Queries.getCurrentTournamentName(connection)%></li>

      <li><a href="wiki/wiki/InstructionsForRunningTheSoftware.html">Instructions (from Wiki)</a></li>

      <li><a href="wiki/wiki/WikiStart.html">Wiki Documentation</a></li>

      <li><a href="scoreEntry/select_team.jsp">Score Entry</a></li>

      <li><a href='scoreboard/index.jsp'>Scoreboard</a></li>

      <li><a href="playoff/index.jsp">Playoffs</a></li>

      <li><a href="report/index.jsp">Tournament reporting</a></li>

      <li><a href="admin/index.jsp">Administration</a></li>

      <li><a href='display.jsp'>Big Screen Display</a>  Follow this link on the computer that's used to display scores with the projector.</li>

      <li><a href="subjective-app.jar">Subjective Scoring Application</a> (Executable Jar file) - run with "java -jar subjective-app.jar"</li>

      <li><a href='playoff/ScoresheetServlet'>Blank scoresheet for printing (PDF format)</a></li>

      <li><a href='challenge.xml'>Challenge Descriptor</a></li>

      <li><a href="<c:url value='/setup'/>">Go to database setup</a></li>
      
      <li><a href="developer/index.jsp">Developer page</a></li>

      <li><a href="troubleshooting/index.jsp">Troubleshooting</a></li>

      <li><a href="credits/credits.jsp">Credits</a></li>

      <li>Addresses to access this page:
        <ul>
          <c:forEach items="${urls}" var="url">
            <li><a href="${url }">${url }</a></li>
          </c:forEach>
        </ul>
      </li>
      
      <li><a href="DoLogout">Log out</a></li>
    </ul>

  </body>
</html>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<%@ page import="fll.web.admin.UploadTeams" %>

<%@ page import="org.w3c.dom.Document" %>
  
<%@ page import="java.sql.Connection" %>

<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");

if(null == session.getAttribute("columnSelectOptions")) {
  throw new RuntimeException("Error columnSelectOptions not set.  Please start back at administration page and go forward.");
}
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Verify Teams)</title>
  </head>

  <body>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Verify Teams)</h1>

    <% if(UploadTeams.verifyTeams(connection, request, response, session, out)) { %>
    <p>Apparently everything uploaded ok.  You probably want to go back to the
    <a href="index.jsp">adminitration menu</a> now.</p>
    <% } %>

<%
Queries.populateTournamentTeams(application);
%>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

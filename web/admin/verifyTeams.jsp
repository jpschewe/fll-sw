<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.web.admin.UploadTeams" %>

<%@ page import="java.sql.Connection" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");

if(null == session.getAttribute("columnSelectOptions")) {
  throw new RuntimeException("Error columnSelectOptions not set.  Please start back at administration page and go forward.");
}
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Verify Teams)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Verify Teams)</h1>

    <% if(UploadTeams.verifyTeams(connection, request, response, session, out)) { %>
    <p>Apparently everything uploaded ok.  You probably want to go back to the
    <a href="index.jsp">adminitration menu</a> now.</p>
    <% } %>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

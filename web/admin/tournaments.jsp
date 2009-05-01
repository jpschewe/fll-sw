<%@ page import="fll.web.admin.Tournaments" %>
  
<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Edit Tournaments)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Edit Tournaments)</h1>
<% Tournaments.generatePage(out, session, request, response); %>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
</body></html>

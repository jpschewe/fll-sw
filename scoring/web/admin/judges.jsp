

<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.web.admin.Judges" %>
  
<%@ page import="org.w3c.dom.Document" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Judge Assignments)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Judge Assignments)</h1>
<% Judges.generatePage(out, application, request, response); %>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
</body></html>

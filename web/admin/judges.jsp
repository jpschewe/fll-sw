
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<%@ page import="fll.web.admin.Judges" %>
  
<%@ page import="org.w3c.dom.Document" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Judge Assignments)</title>
  </head>

  <body background='../images/bricks1.gif' bgcolor='#ffffff' topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Judge Assignments)</h1>
<% Judges.generatePage(out, application, request, response); %>
<%@ include file="../WEB-INF/jspf/footer.jspf" %>
</body></html>
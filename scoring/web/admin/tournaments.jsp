<%@ page errorPage="../errorHandler.jsp" %>
  
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ page import="fll.web.admin.Tournaments" %>
  
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Edit Tournaments)</title>
  </head>

  <body background='<c:url value="/images/bricks1.gif"/>' bgcolor='#ffffff' topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Edit Tournaments)</h1>

<%
final String unknownTournament = request.getParameter("unknownTournament");
if(null != unknownTournament) {
%>
  <p><font color='red'>You specified an unknown tournament: <%=unknownTournament%><br>
  Would you like to enter it?</p>
<%}%>

<% Tournaments.generatePage(out, application, request, response); %>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
</body></html>
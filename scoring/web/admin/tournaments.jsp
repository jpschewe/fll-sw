
  
<%@ page import="fll.web.admin.Tournaments" %>
  
<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Edit Tournaments)</title>
  </head>

  <body>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Edit Tournaments)</h1>
      <c:if test="${not empty param.unknownTournament}">
        <p><font color='red'>You specified an unknown tournament: <c:out value="${param.unknownTournament}"/><br>
                Would you like to enter it?</font></p>
      </c:if>

<% Tournaments.generatePage(out, application, request, response); %>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
</body></html>

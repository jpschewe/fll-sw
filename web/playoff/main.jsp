<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<%@ page import="org.w3c.dom.Document" %>

<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final String divisionStr = request.getParameter("division");
if(null == divisionStr) {
  throw new RuntimeException("No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
}
%>

<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Playoff Brackets) Division: <%=divisionStr%></title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>
  <frameset rows="80,*" border='1' framespacing='0'>
      <frame name='title' src='title.jsp' marginheight='0' marginwidth='0' scrolling='no'>
      <frame name='brackets' src='brackets.jsp?<%=request.getQueryString()%>' marginwidth='0' scrolling='no'>
</frameset>

</HTML>

<%@ page errorPage="../errorHandler.jsp" %>
  
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

  
<%@ page import="org.w3c.dom.Document" %>

<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<HTML>
  <HEAD>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Scoreboard)</title>
  </HEAD>
  <body background="<c:url value="/images/bricks1.gif" />" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Scoreboard)</h1>
    <ul>
      <li><a href='<c:url value="main.jsp" />'>Primary Scoreboard (1024x768)</a> (requires Internet Explorer)</li>
      <li><a href='<c:url value="/scoreboard_800/main.jsp" />'>Primary Scoreboard
            (800x600)</a> (requires Internet Explorer)</li>
        
      <li><a href='<c:url value="allteams.jsp"><c:param name="scroll" value="false"/></c:url>'>All Teams, All Runs (primarily for internal use)</a></li>
        
    </ul>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </BODY>
</HTML>

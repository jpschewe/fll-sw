
  
<%@ include file="/WEB-INF/jspf/init.jspf" %>

  
<%@ page import="org.w3c.dom.Document" %>

<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
  
<HTML>
  <HEAD>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Scoreboard)</title>
  </HEAD>
  <body>
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

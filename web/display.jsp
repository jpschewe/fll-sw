<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="org.w3c.dom.Document" %>
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>
<html>
  <head>
    <meta http-equiv='refresh' content='60' />
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%></title>
          
    <script type='text/javascript'>
      var width = screen.width-10;
      var height = screen.height-10;
      var newWindow = null;
      var str = 'height='+height+',width='+width+',toolbar=0,menubar=0,scrollbars=0,location=0,directories=0,status=0,resizable=0,fullscreen=1,left=0,screenX=0,top=0,screenY=0';
      newWindow = window.open('welcome.jsp', 'displayWindow', str);

      <c:choose>
        <c:when test='${displayPage == "scoreboard"}'>
          newWindow.location.href = '<c:url value="/scoreboard/main.jsp"/>';
        </c:when>
        <c:when test='${displayPage == "playoffs"}'>
          newWindow.location.href = '<c:url value="/playoff/remoteMain.jsp"/>';
        </c:when>
        <c:otherwise>
          newWindow.location.href = '<c:url value="/welcome.jsp"/>';
        </c:otherwise>
      </c:choose>
    </script>
  </head>

  <body>
      
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%></h1>
    
    <p>Leave this page open on the display computer.  It's used to control
    the actual display window.  You may need to press F11 in the newly
    opened window to remove the titlebar and make it fullscreen.</p>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

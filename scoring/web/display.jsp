<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <meta http-equiv='refresh' content='60' />
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    
    <title><x:out select="$challengeDocument/fll/@title"/></title>
          
    <script type='text/javascript'>
      var width = screen.width-10;
      var height = screen.height-10;
      var newWindow = null;
      var str = 'height='+height+',width='+width+',toolbar=0,menubar=0,scrollbars=0,location=0,directories=0,status=0,resizable=0,fullscreen=1,left=0,screenX=0,top=0,screenY=0';

      <c:if test="${empty sessionDisplayPage}">
        <c:set var="sessionDisplayPage" value="none" scope="session"/>
      </c:if>
          
      <c:if test="${displayPage != sessionDisplayPage}">
        <c:choose>
          <c:when test='${displayPage == "scoreboard"}'>
            newWindow = window.open('<c:url value="/scoreboard/main.jsp"/>', 'displayWindow', str);
          </c:when>
          <c:when test='${displayPage == "playoffs"}'>
            newWindow = window.open('<c:url value="/playoff/remoteMain.jsp"/>', 'displayWindow', str);
          </c:when>
          <c:otherwise>
            newWindow = window.open('<c:url value="/welcome.jsp"/>', 'displayWindow', str);
          </c:otherwise>
        </c:choose>
        <c:set var="sessionDisplayPage" value="${displayPage}" scope="session"/>
      </c:if>
      
    </script>
  </head>

  <body>
      
    <h1><x:out select="$challengeDocument/fll/@title"/></h1>
    
    <p>Leave this page open on the display computer.  It's used to control
    the actual display window.  You may need to press F11 in the newly
    opened window to remove the titlebar and make it fullscreen.</p>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

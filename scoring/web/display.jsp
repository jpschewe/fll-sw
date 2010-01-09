<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities"%>


<html>
  <head>
    <meta http-equiv='refresh' content='60' />
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    
    <title>Display</title>
          
    <script type='text/javascript'>
      var width = screen.width-10;
      var height = screen.height-10;
      var newWindow = null;
      var str = 'height='+height+',width='+width+',toolbar=0,menubar=0,scrollbars=0,location=0,directories=0,status=0,resizable=0,fullscreen=1,left=0,screenX=0,top=0,screenY=0';

      <%-- Default the session variables that keep track of what we're displaying to none --%>
      <c:if test="${empty sessionDisplayPage}">
        <c:set var="sessionDisplayPage" value="none" scope="session"/>
      </c:if>
      <c:if test="${empty sessionDisplayURL}">
        <c:set var="sessionDisplayURL" value="none" scope="session" />
      </c:if>

      <%-- Set localDisplayPage and localDisplayURL to the values depending on displayPage --%>
      <c:choose>
        <c:when test='${not empty displayName}'>
          <c:set var='displayPageKey' value='${displayName}_displayPage'/>
          <c:set var='displayURLKey' value='${displayName}_displayURL'/>

          <c:choose>
            <c:when test='${not empty applicationScope[displayPageKey]}'>
              <c:set var='localDisplayPage' value='${applicationScope[displayPageKey]}'/>
            </c:when>
            <c:otherwise>
              <c:set var='localDisplayPage' value='${displayPage}'/>
            </c:otherwise>
          </c:choose>
          
          <c:choose>
            <c:when test='${not empty applicationScope[displayURLKey]}'>
              <c:set var='localDisplayURL' value='${applicationScope[displayURLKey]}'/>
            </c:when>
            <c:otherwise>
              <c:set var='localDisplayURL' value='${displayURL}'/>
            </c:otherwise>
          </c:choose>
          
        </c:when> <%-- end not empty displayName --%>
        <c:otherwise>
          <c:set var='localDisplayPage' value='${displayPage}'/>
          <c:set var='localDisplayURL' value='${displayURL}'/>
        </c:otherwise>
      </c:choose>
      

      <%-- Now change the display based on the current state and store that information in the session --%> 
      <c:if test='${localDisplayPage != sessionDisplayPage || (localDisplayPage == "special" && localDisplayURL != sessionDisplayURL)}'>
        <c:choose>
          <c:when test='${localDisplayPage == "scoreboard"}'>
            newWindow = window.open('<c:url value="/scoreboard/main.jsp"/>', 'displayWindow', str);
            <c:set var="sessionDisplayURL" value="none" scope="session" />
          </c:when>
          <c:when test='${localDisplayPage == "slideshow"}'>
            newWindow = window.open('<c:url value="/slideshow/index.jsp"/>', 'displayWindow', str);
            <c:set var="sessionDisplayURL" value="none" scope="session" />
          </c:when>
          <c:when test='${localDisplayPage == "playoffs"}'>
            newWindow = window.open('<c:url value="/playoff/remoteMain.jsp"/>', 'displayWindow', str);
            <c:set var="sessionDisplayURL" value="none" scope="session" />
          </c:when>
          <c:when test='${localDisplayPage == "special"}'>
            newWindow = window.open('<c:url value="${displayURL}"/>', 'displayWindow', str);
            <c:set var="sessionDisplayURL" value="${localDisplayURL}" scope="session" />
          </c:when>
          <c:otherwise>
            newWindow = window.open('<c:url value="/welcome.jsp"/>', 'displayWindow', str);
          </c:otherwise>
        </c:choose>
        <c:set var="sessionDisplayPage" value="${localDisplayPage}" scope="session"/>
      </c:if>
      
    </script>
  </head>

  <body>
      
    <h1><x:out select="$challengeDocument/fll/@title"/></h1>
    
<c:if test="${not empty param.name}">
  <c:set var="displayName" scope="session">
    ${param.name}
  </c:set>
  <% Utilities.appendDisplayName(application, request.getParameter("name")); %>
  <p>Display set to ${param.name}</p>
</c:if>

    <c:if test="${not empty displayName}">
      <p>This display is named ${displayName}</p>
    </c:if>
    
    Displaying page ${sessionDisplayPage}.<br/>

    <p>Leave this page open on the display computer.  It's used to control
    the actual display window.  You may need to press F11 in the newly
    opened window to remove the titlebar and make it fullscreen.</p>
    
    <form action="display.jsp" method="POST">
      Name this display computer: <input name="name" type="text" size="40"/><br/>
      <input type='submit' value='Submit'/>
    </form>


  </body>
</html>

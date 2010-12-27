<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Set remote control params</title></head>
<body>

   
<%-- DEBUG
<ul>
<li>displayPage - ${param.remotePage }</li>
<li>displayURL - ${param.remoteURL }</li>
<li>playoffDivision - ${param.playoffDivision }</li>
<li>playoffRoundNumber - ${param.playoffRoundNumber }</li>

    <c:if test="${not empty displayNames}">
      <c:forEach items="${displayNames}" var="displayName">
  <c:set var="remotePageKey" value="${displayName}_remotePage" />
  <c:set var="remoteURLKey" value="${displayName}_remoteURL" />
                  <c:set var="playoffDivisionKey" value="${displayName}_playoffDivision" />
              <c:set var="playoffRoundNumberKey" value="${displayName}_playoffRoundNumber" />
  
<li>Display - ${displayName}
<ul>
<li>displayPage - ${param[remotePageKey]}</li>
<li>displayURL - ${param[remoteURLKey] }</li>
<li>playoffDivision - ${param[playoffDivisionKey] }</li>
<li>playoffRoundNumber - ${param[playoffRoundNumberKey] }</li>
</ul>
</li>
</c:forEach>
</c:if>

</ul>
END DEBUG --%>

<%-- convert params from remoteControl.jsp into the appropriate application variables --%>


<%-- slide show interval is not display specific --%>
<c:choose>
 <c:when test="${empty param.slideInterval}">
  <c:set var='slideShowInterval' value='10' scope='application' />
 </c:when>
 <c:otherwise>
  <c:set var='slideShowInterval' value='${Integer.valueOf(param.slideInterval)}'
   scope='application' />
 </c:otherwise>
</c:choose>


<%-- handle default values--%>
<c:set var='displayPage' value="${param.remotePage}" scope='application' />
<c:set var='displayURL' value="${param.remoteURL}" scope='application' />
<c:set var='playoffRoundNumber' value="${param.playoffRoundNumber}"
 scope='application' />
<c:set var='playoffDivision' value="${param.playoffDivision}"
 scope='application' />


<%-- handle values for specific displays --%>
<c:if test="${not empty displayNames}">
 <c:forEach items="${displayNames}" var="displayName">
  <c:set var="remotePageKey" value="${displayName}_remotePage" />

  <c:choose>
   <c:when test="${param[remotePageKey] == 'default' }">
    <%
      String displayName = (String) pageContext.getAttribute("displayName");
              application.removeAttribute(displayName
                  + "_displayPage");
              application.removeAttribute(displayName
                  + "_displayURL");
              application.removeAttribute(displayName
                  + "_playoffDivision");
              application.removeAttribute(displayName
                  + "_playoffRoundNumber");
    %>
   </c:when>
   <c:otherwise>
    <%
      String displayName = (String) pageContext.getAttribute("displayName");
              application.setAttribute(displayName
                  + "_displayPage", request.getParameter(displayName
                  + "_remotePage"));
              application.setAttribute(displayName
                  + "_displayURL", request.getParameter(displayName
                  + "_remoteURL"));
              application.setAttribute(displayName
                  + "_playoffDivision", request.getParameter(displayName
                  + "_playoffDivision"));
              application.setAttribute(displayName
                  + "_playoffRoundNumber", Integer.valueOf(request.getParameter(displayName
                  + "_playoffRoundNumber")));
    %>
   </c:otherwise>
  </c:choose>

 </c:forEach>
</c:if>

<c:set var="message" value="<i>Successfully set remote control parameters</i>" scope="application"/>

Should redirect to
<a href="remoteControl.jsp">remote control</a>
.

<c:redirect url="remoteControl.jsp"/>
</body>
</html>

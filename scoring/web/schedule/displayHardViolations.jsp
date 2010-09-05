<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Upload Schedule</title>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title"/> (Upload Schedule)</h1>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<p>
There are some errors with this schedule, please correct them and try again.  
</p>
 
<ul>
<c:forEach items="${uploadSchedule_violations}" var="violation">
  <c:choose>
    <c:when test="${violation.hard }">
      <li class='hard-violation'>${violation.message }</li>
    </c:when>
    <c:otherwise>
      <li class='soft-violation'>${violation.message }</li>
    </c:otherwise>
  </c:choose>
</c:forEach> 
</ul>

<p>
<a href='<c:url value="/admin/index.jsp"/>'>Return to admin page</a><br/>
</p>

</body>
</html>

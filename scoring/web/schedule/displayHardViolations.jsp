<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Upload Schedule</title>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
<h1>Upload Schedule</h1>

<div class='status-message'>${message}</div>
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<p>
There are some errors with this schedule, please correct them and try again.  
</p>
 
<ul>
<c:forEach items="${uploadScheduleData.violations}" var="violation">
  <c:choose>
    <c:when test="${violation.type == 'HARD' }">
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

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
There are some warnings in the schedule.  
</p>
 
<ul>
<c:forEach items="${uploadSchedule_violations}" var="violation">
  <li>${violation.message }</li>
</c:forEach> 
</ul>

<p>
Do you want to proceed and use this schedule?
<a href='<c:url value="/schedule/CommitSchedule"/>'>Yes</a><br/>
<a href='<c:url value="/admin/index.jsp"/>'>No</a><br/>
</p>

</body>
</html>

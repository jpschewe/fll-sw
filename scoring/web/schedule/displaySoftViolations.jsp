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
There are some warnings in the schedule.  
</p>
 
<ul>
<c:forEach items="${uploadScheduleData.violations}" var="violation">
  <li class='soft-violation'>${violation.message }</li>
</c:forEach> 
</ul>

<p>
Do you want to proceed and use this schedule?
<a id='yes' href='<c:url value="/schedule/promptForEventDivisions.jsp"/>'>Yes</a>
<a id='no' href='<c:url value="/admin/index.jsp"/>'>No</a>
</p>

</body>
</html>

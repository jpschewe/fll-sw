<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Upload Schedule</title>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
<h1>Upload Schedule</h1>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<p>
Would you like to set the award groups based upon the information in the schedule data file? 
You will be prompted with the changes before they are committed.<br/>
<a href='<c:url value="/schedule/GatherEventDivisionChanges"/>'>Yes</a>
<a href='<c:url value="/schedule/CommitSchedule"/>'>No</a>
</p>

</body>
</html>

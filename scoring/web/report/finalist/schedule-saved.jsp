<%@ include file="/WEB-INF/jspf/init.jspf"%>

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Finalist Schedule Saved</title>
</head>

<body>
 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <p>
  The finalist schedule has been saved. You can go back to the <a
   href="schedule.html">finalist schedule</a> or perhaps just want to go
  back to the <a href="<c:url value='/' />">main page</a>.
 </p>

</body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Edit Event Division</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>

<body>

 <form method="POST" action="CommitEventDivision">
  <p>Team ${teamNumber} is assigned to a tournament that is using
   event divisions. You need to specify which event division this team
   is to be in or create a new event division.</p>

  <c:forEach items="${all_event_divisions }" var="ediv">

   <input type='radio' name='<c:out value="event_division"/>'
    value='${ediv }' />

  </c:forEach>

  <input type='radio' name='event_division' value='text' /> <input
   type='text' name='event_division_text' /> <br /> <input type='submit' name='submit'
   value='Commit' />

 </form>

</body>
</html>
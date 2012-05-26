<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />

</head>

<body>
 <h1>
  <x:out select="$challengeDocument/fll/@title" />
 </h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <form name="choose_headers" method='POST'
  action='ProcessSubjectiveHeaders'>
  <p>These are the event divisions that will be assigned along with
   the divisions that the teams are in for the season.</p>
  <table border='1'>
   <tr>
    <th>Team Number</th>
    <th>Team Name</th>
    <th>Season Division</th>
    <th>Event Division</th>
   </tr>
   <c:forEach items="${uploadSchedule_eventDivisionInfo }" var="info">
    <tr>
     <td>${info.teamNumber}</td>
     <td>${info.teamName}</td>
     <td>${info.division}</td>
     <td>${info.eventDivision}</td>
    </tr>
   </c:forEach>
  </table>


  <a href='<c:url value="/schedule/CommitEventDivisions"/>'>Yes,
   these changes look OK</a><br /> <a
   href='<c:url value="/schedule/CommitSchedule"/>'>No, don't assign
   event divisions</a><br /> 
 </form>

</body>
</html>
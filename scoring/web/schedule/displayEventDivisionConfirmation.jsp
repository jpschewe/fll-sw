<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>FLL-SW</title>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/style.jsp'/>" />

</head>

<body>
  <h1>Confirm Award Groups</h1>

  ${message}
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <form
    name="choose_headers"
    method='POST'
    action='ProcessSubjectiveHeaders'>
    <p>These are the award groups that will be assigned. Note that
      this information can be changed later.</p>
    <table border='1'>
      <tr>
        <th>Team Number</th>
        <th>Team Name</th>
        <th>Award Group</th>
      </tr>
      <c:forEach
        items="${uploadSchedule_eventDivisionInfo }"
        var="info">
        <tr>
          <td>${info.teamNumber}</td>
          <td>${info.teamName}</td>
          <td>${info.eventDivision}</td>
        </tr>
      </c:forEach>
    </table>


    <a href='<c:url value="/schedule/CommitEventDivisions"/>'>Yes,
      these changes look OK</a><br /> <a
      href='<c:url value="/schedule/CommitSchedule"/>'>No, don't
      assign award groups</a><br />
  </form>

</body>
</html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>


<html>
<head>
<title>Assign Teams to Tournament</title>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
  <h1>${challengeDescription.title }(Assign Teams to Tournament)</h1>

  <p>Select the column that specifies the team numbers and the
    column for the tournament. Highlighted columns are required, all
    others are optional.</p>

  <form
    action="ProcessTeamTournamentAssignmentsUpload"
    method="POST"
    name="columnSelection">
    <table border='1'>

      <tr bgcolor='yellow'>
        <td>TeamNumber</td>
        <td><select name='teamNumber'>
            <c:forEach
              items="${fileHeaders }"
              var="fileHeader">
              <option value="${fileHeader }">${fileHeader }</option>
            </c:forEach>
        </select></td>
      </tr>

      <tr bgcolor='yellow'>
        <td>Tournament</td>
        <td><select name='tournament'>
            <c:forEach
              items="${fileHeaders }"
              var="fileHeader">
              <option value="${fileHeader }">${fileHeader }</option>
            </c:forEach>
        </select></td>
      </tr>

      <tr>
        <td>Event Division</td>
        <td><select name='event_division'>
            <c:forEach
              items="${fileHeaders }"
              var="fileHeader">
              <option value="${fileHeader }">${fileHeader }</option>
            </c:forEach>
        </select></td>
      </tr>

      <tr>
        <td>Judging Group</td>
        <td><select name='judging_station'>
            <c:forEach
              items="${fileHeaders }"
              var="fileHeader">
              <option value="${fileHeader }">${fileHeader }</option>
            </c:forEach>
        </select></td>
      </tr>
            

      <tr>
        <td colspan='2'><input
          type='submit'
          value='Submit'></td>
      </tr>

    </table>
  </form>


</body>
</html>

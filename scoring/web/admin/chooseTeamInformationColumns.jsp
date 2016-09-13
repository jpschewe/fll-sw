<%@ include file="/WEB-INF/jspf/init.jspf"%>


<html>
<head>
<title>Update Team Information</title>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
  <h1>${challengeDescription.title }(UpdateTeamInformation)</h1>

  <p>Select the columns from the data file that match the team
    information attributes. Highlighted columns are required, all others
    are optional.</p>

  <form
    action="ProcessTeamInformationUpload"
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


      <tr>
        <td>Team Name</td>
        <td><select name='teamName'>
            <option
              value=''
              selected>None</option>

            <c:forEach
              items="${fileHeaders }"
              var="fileHeader">
              <option value="${fileHeader }">${fileHeader }</option>
            </c:forEach>
        </select></td>
      </tr>

      <tr>
        <td>Organization</td>
        <td><select name='organization'>
            <option
              value=''
              selected>None</option>

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

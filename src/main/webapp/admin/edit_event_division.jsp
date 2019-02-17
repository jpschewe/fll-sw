<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.admin.CommitAwardGroups.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Edit Award Groups</title>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
  <h1>Edit Award Groups</h1>

  <div class='status-message'>${message}</div>
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <p>This page allows you to assign teams to award groups. Below are
    the teams at the current tournament and what award group they are
    in. You can either select one of the existing award groups or select
    the text field and enter a new award group.</p>

  <form
    name='edit_event_divisions'
    action='CommitAwardGroups'
    method='post'>
    <table
      id='data'
      border='1'>
      <tr>
        <th>Team Number</th>
        <th>Team Name</th>
        <th>Award Group</th>
      </tr>
      <c:forEach
        items="${teams}"
        var="team">
        <tr>
          <td><c:out value="${team.teamNumber}" /></td>
          <td><c:out value="${team.teamName}" /></td>

          <td><c:forEach
              items="${divisions}"
              var="division">
              <c:choose>
                <c:when test="${team.awardGroup == division}">
                  <input
                    type='radio'
                    name='<c:out value="${team.teamNumber}"/>'
                    value='<c:out value="${division}"/>'
                    checked />
                </c:when>
                <c:otherwise>
                  <input
                    type='radio'
                    name='<c:out value="${team.teamNumber}"/>'
                    value='<c:out value="${division}"/>' />
                </c:otherwise>
              </c:choose>
              <c:out value="${division}" />
            </c:forEach> <input
            type='radio'
            name='<c:out value="${team.teamNumber}"/>'
            value='text' /> <input
            type='text'
            name='<c:out value="${team.teamNumber}"/>_text' /></td>
        </tr>
      </c:forEach>
    </table>

    <input
      type='submit'
      name='submit'
      value='Commit' /> <input
      type='submit'
      name='submit'
      value='Cancel' />
  </form>


</body>
</html>

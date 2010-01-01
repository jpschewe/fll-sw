<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if test="${empty dbimport}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>dbimport</tt> parameter missing from session (from addMissingTeams)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<c:if test="${empty selectedTournament}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>selectedTournament</tt> parameter missing from session (from addMissingTeams)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<c:if test="${empty missingTeams}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>missingTeams</tt> parameter missing from session (from addMissingTeams)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<%-- end if not proper workflow --%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Add missing teams</title>
</head>

<body>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<form name="promptCreateMissingTeams" action="AddMissingTeams">
<p>The following teams are in the source database and not in the dest database. OK to add teams?</p>

<table>
  <tr><th>Team Number</th><th>Team Name</th></tr>
  <c:forEach items="${missingTeams}" var="team">
    <tr>
      <td>${team.teamNumber}</td>
      <td>${team.teamName}</td>
    </tr> 
  </c:forEach>
</table>
 
<input name='submit' type='submit' value="Add teams"/>
</form>

<p>Return to the <a href="selectTournament.jsp">tournament selection page</a> if you do not want to add the teams.</p>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>

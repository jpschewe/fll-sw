<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="fll.web.SessionAttributes"%>

<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
pageContext.setAttribute("currentTournament", Queries.getCurrentTournament(connection));
%>
<%-- check for form submission --%>
<c:if test="${param.submit == 'Cancel'}">
 <!--  redirect to index.jsp -->
 <c:redirect url="index.jsp">
  <c:param name="message">Canceled changes to event divisions</c:param>
 </c:redirect>
</c:if>
<c:if test="${param.submit == 'Commit'}">
 <%--  form submission --%>
 <sql:query var="result" dataSource="${datasource}">
  SELECT TeamNumber
    FROM current_tournament_teams
</sql:query>
 <c:forEach items="${result.rows}" var="row">
  <c:set var="teamNumber" scope="page">
   <c:out value="${row.TeamNumber}" />
  </c:set>
  <!-- 
      checking against empty <c:out value="${paramValues[teamNumber][0]}"/> 
      team number <c:out value="${teamNumber}"/> 
 -->
  <c:if test="${not empty paramValues[teamNumber][0]}">
   <c:choose>
    <c:when test="${paramValues[teamNumber][0] eq 'text'}">
     <!--  inside when - is text -->
     <c:set var="division_param" scope="page">
      <c:out value="${teamNumber}" />_text    
     </c:set>
     <c:set var="new_division" value="${paramValues[division_param][0]}"
      scope="page" />
    </c:when>
    <c:otherwise>
     <c:set var="new_division" value="${paramValues[teamNumber][0]}"
      scope="page" />
    </c:otherwise>
   </c:choose>
   <!--  setting event division for <c:out value="${teamNumber}"/> 
         to <c:out value="${new_division}"/> 
         in tournament <c:out value="${currentTournament}"/> -->
   <sql:update dataSource="${datasource}">
    UPDATE TournamentTeams 
      SET event_division = '<c:out value="${new_division}" />' 
      WHERE TeamNumber = <c:out value="${teamNumber}" />
      AND Tournament = <c:out value="${currentTournament}" />
  </sql:update>
  </c:if>
 </c:forEach>
</c:if>

<html>
<head>
<title>Administration</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /> (Edit Event
Division)</h1>

<p>This page allows you to assign event divisions to each team.
Normally teams are just in the divisions they are registered in. However
at some tournaments the teams are in different divisions to allow one to
run effectively two tournaments at the same location. Below are the
teams at the current tournament and what event division they are in. You
can either select one of the existing divisions or select the text field
and enter a new division name. Once you've entered a new division name
you may press commit at the bottom and this division name will be added
to the radio buttons.</p>

<sql:query var="result" dataSource="${datasource}">
SELECT Teams.TeamNumber, Teams.TeamName, current_tournament_teams.event_division 
  FROM current_tournament_teams, Teams 
  WHERE current_tournament_teams.TeamNumber = Teams.TeamNumber 
  ORDER BY Teams.TeamNumber
</sql:query>
<form name='edit_event_divisions' action='edit_event_division.jsp'
 method='post'>
<table id='data' border='1'>
 <tr>
  <th>Team Number</th>
  <th>Team Name</th>
  <th>Division</th>
 </tr>
 <c:forEach items="${result.rows}" var="row">
  <tr>
   <td><c:out value="${row.TeamNumber}" /></td>
   <td><c:out value="${row.TeamName}" /></td>
   <%
   	pageContext.setAttribute("divisions", Queries.getEventDivisions(connection));
   %>

   <td>
   <c:forEach items="${divisions}" var="division">
    <c:choose>
     <c:when test="${row.event_division == division}">
      <input type='radio' name='<c:out value="${row.TeamNumber}"/>'
       value='<c:out value="${division}"/>' checked />
     </c:when>
     <c:otherwise>
      <input type='radio' name='<c:out value="${row.TeamNumber}"/>'
       value='<c:out value="${division}"/>' />
     </c:otherwise>
    </c:choose>
    <c:out value="${division}" />
   </c:forEach>
   <input type='radio' name='<c:out value="${row.TeamNumber}"/>'
    value='text' /> <input type='text'
    name='<c:out value="${row.TeamNumber}"/>_text' />
    </td>
  </tr>
 </c:forEach>
</table>

<input type='submit' name='submit' value='Commit' /> <input
 type='submit' name='submit' value='Cancel' /></form>


</body>
</html>

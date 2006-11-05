<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.Queries"%>

<%@ page import="java.sql.Connection"%>

<%
      Connection connection = (Connection) application.getAttribute("connection");
      pageContext.setAttribute("currentTournament", Queries.getCurrentTournament(connection));
%>
<c:if test="${param.submit == 'Commit'}">
 <%--  form submission --%>
 <sql:query var="result" dataSource="${datasource}">
  SELECT TeamNumber
    FROM TournamentTeams
    WHERE Tournament = '<c:out value="${currentTournament}" />'
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
      AND Tournament = '<c:out value="${currentTournament}" />'
  </sql:update>
  </c:if>
 </c:forEach>
</c:if>

<html>
<head>
<title><x:out select="$challengeDocument/fll/@title" />
(Administration)</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /> (Edit Event
Division)</h1>


<sql:query var="result" dataSource="${datasource}">
SELECT Teams.TeamNumber, Teams.TeamName, TournamentTeams.event_division 
  FROM TournamentTeams, Teams 
  WHERE TournamentTeams.TeamNumber = Teams.TeamNumber 
  AND Tournament = '<c:out value="${currentTournament}" />'
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
   pageContext.setAttribute("divisions", Queries.getDivisions(connection));
   %>

   <td><c:forEach items="${divisions}" var="division">
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
   </c:forEach> <input type='radio' name='<c:out value="${row.TeamNumber}"/>'
    value='text' /> <input type='text'
    name='<c:out value="${row.TeamNumber}"/>_text' /></td>
  </tr>
 </c:forEach>
</table>

<input type='submit' name='submit' value='Commit' /> <input
 type='submit' name='submit' value='Cancel' /></form>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>

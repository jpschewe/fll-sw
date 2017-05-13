<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.report.finalist.PublicFinalistDisplaySchedule
					.populateContext(request, application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/base.css'/>" />
<script type='text/javascript'
 src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>

<link rel='stylesheet' type='text/css' href='../../style/base.css' />
<link rel='stylesheet' type='text/css' href='../../scoreboard/score_style.css' />

<script type='text/javascript' src="<c:url value='/scripts/scroll.js'/>"></script>

<script type="text/javascript">
	$(document).ready(function() {
		<c:if test="${param.finalistScheduleScroll}">
		startScrolling();
		</c:if>
	});
</script>

</head>

<body class='scoreboard'>
 <div class='status-message'>${message}</div>
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <h1>Finalist Schedule for Award Group: ${division }</h1>

 <c:forEach items="${publicCategories }" var="category">
  <c:choose>
   <c:when test="${empty rooms[category] }">
    <h2>Category: ${category }</h2>
   </c:when>
   <c:otherwise>
    <h2>Category: ${category } Room: ${rooms[category] }</h2>
   </c:otherwise>
  </c:choose>

  <c:forEach items="${publicSchedules[category] }" var="schedRow">
   <div>
    <c:set var="team" value="${allTeams[schedRow.teamNumber] }" />

    <span><fmt:formatDate value="${schedRow.time }" type="time"
      pattern="hh:mm" /></span> - ${team.teamNumber } - ${team.teamName } -
    ${team.organization }
   </div>
  </c:forEach>

 </c:forEach>

</body>
</html>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.report.finalist.FinalistTeams.populateContext(application, pageContext);
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/base.css'/>" />

<script
  type='text/javascript'
  src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>

<link
  rel='stylesheet'
  type='text/css'
  href='../../style/base.css' />
<link
  rel='stylesheet'
  type='text/css'
  href='../../scoreboard/score_style.css' />


<script
  type='text/javascript'
  src="<c:url value='/scripts/scroll.js'/>"></script>

<script type="text/javascript">
  $(document).ready(function() {
    <c:if test="${param.finalistTeamsScroll}">
    startScrolling();
    </c:if>
  });
</script>

</head>

<body class='scoreboard'>
  <div class='status-message'>${message}</div>
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <h1>Teams in finalist judging</h1>

  <p>Please send a coach to the information desk to pickup your
    finalist schedule.</p>

  <table border='1'>
    <tr>
      <th>Team #</th>
      <th>Team Name</th>
      <th>Organization</th>
    </tr>

    <c:forEach
      items="${teams }"
      var="team">
      <tr>
        <td>${team.teamNumber}</td>
        <td>${team.teamName}</td>
        <td>${team.organization}</td>
      </tr>
    </c:forEach>

  </table>

</body>
</html>

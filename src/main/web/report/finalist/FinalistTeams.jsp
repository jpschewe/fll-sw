<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.report.finalist.FinalistTeams.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/base.css'/>" />

<link rel='stylesheet' type='text/css' href='../../style/base.css' />
<link rel='stylesheet' type='text/css'
    href='../../scoreboard/score_style.css' />


<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type="text/javascript"
    src="<c:url value='/extlib/jquery.scrollTo/jquery.scrollTo.min.js'/>"></script>

<script type="text/javascript">
  var scrollDuration = parseInt("${scrollDuration}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree

  function bottomReload() {
    console.log("In bottomReload");
    $.scrollTo($("#top"));
    location.reload(true);
  }

  function scrollToBottom() {
    $.scrollTo($("#bottom"), {
      duration : scrollDuration,
      easing : 'linear',
      onAfter : bottomReload,
    });
  }

  $(document).ready(function() {
    <c:if test="${param.finalistTeamsScroll}">
    console.log("Starting scroll");
    scrollToBottom();
    </c:if>
  });
</script>

</head>

<body class='scoreboard'>
    <div id="top">
        &nbsp;
    </div>

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

        <c:forEach items="${teams }" var="team">
            <tr>
                <td>${team.teamNumber}</td>
                <td>${team.teamName}</td>
                <td>${team.organization}</td>
            </tr>
        </c:forEach>

    </table>

    <div id="bottom">
        &nbsp;
    </div>
</body>
</html>

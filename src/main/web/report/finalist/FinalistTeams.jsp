<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.report.finalist.FinalistTeams.populateContext(application, pageContext);
%>

<html>
<head>
<title>Finalist teams</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/base.css'/>" />

<link rel='stylesheet' type='text/css'
    href='<c:url value="/scoreboard/score_style.css"/>' />

<link rel='stylesheet' type='text/css' href="finalist_teams.css" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript">
  let prevScrollTimestamp = 0;
  const secondsBetweenScrolls = parseFloat("${scrollRate}");
  // if less than 2, Chromeboook doesn't scroll
  const pixelsToScroll = 2;

  function reload() {
    window.scrollTo(0, 0);
    location.reload(true);
  }

  function scrollDown(timestamp) {
    if (!elementIsVisible(document.getElementById("bottom"))) {
      const diff = timestamp - prevScrollTimestamp;
      if (diff / 1000.0 >= secondsBetweenScrolls) {
        window.scrollBy(0, pixelsToScroll);
        prevScrollTimestamp = timestamp;
      }
      requestAnimationFrame(scrollDown);
    } else {
      // show the bottom for a bit and then reload
      setTimeout(reload, 3000);
    }
  }

  document.addEventListener("DOMContentLoaded", function() {
    <c:if test="${param.finalistTeamsScroll}">
    console.log("Starting scroll");
    requestAnimationFrame(scrollDown);
    </c:if>
  });
</script>

</head>

<body class='scoreboard'>
    <%@ include file="/WEB-INF/jspf/message.jspf"%>
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

    <div id="bottom">&nbsp;</div>
</body>
</html>

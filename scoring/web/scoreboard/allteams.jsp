<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.scoreboard.AllTeams.populateContext(application, session, pageContext);
%>

<html>
<head>
<link
  rel='stylesheet'
  type='text/css'
  href='../style/base.css' />
<link
  rel='stylesheet'
  type='text/css'
  href='score_style.css' />

<style>
TABLE.A {
	background-color: #000080
}

TABLE.B {
	background-color: #0000d0
}
</style>

<script
  type='text/javascript'
  src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script
  type="text/javascript"
  src="<c:url value='/extlib/jquery.scrollTo-2.1.2.min.js'/>"></script>



<script type="text/javascript">
  function reload() {
    $.scrollTo($("#top"));
    location.reload(true);
  }
  function bottomReload() {
    // show the last scores for a bit and then reload
    setTimeout(reload, 3000);
  }
</script>

<c:if test="${param.allTeamsScroll}">
  <c:choose>
    <c:when test="${fn:length(teamsWithScores) gt 0}">
      <script type="text/javascript">
              var scrollDuration = parseInt("${scrollDuration}"); // could be here directly as an intger, but the JSTL and auto-formatting don't agree

              function scrollToBottom() {
                $.scrollTo($("#bottom"), {
                  duration : scrollDuration,
                  easing : 'linear',
                  onAfter : bottomReload,
                });
              }

              $(document).ready(function() {
                scrollToBottom();
              });
            </script>
    </c:when>
    <c:otherwise>
      <script type="text/javascript">
              $(document).ready(function() {
                // reload every 5 seconds
                window.setInterval('reload()', 5000);
              });
            </script>
    </c:otherwise>
  </c:choose>
</c:if>


</head>

<body class='scoreboard'>
  <span id="top">&nbsp;</span>

  <br />
  <br />
  <br />
  <br />
  <br />
  <br />
  <br />
  <br />

  <c:set
    var="colorStr"
    value="A" />


  <c:choose>
    <c:when test="${fn:length(teamsWithScores) gt 0}">
      <c:forEach
        items="${teamsWithScores }"
        var="team"
        varStatus="loopStatus">
        <c:set
          var="teamScores"
          value="${scores[team.teamNumber] }" />
        <c:set
          var="teamIndex"
          value="${loopStatus.index }" />

        <table
          border='0'
          cellpadding='0'
          cellspacing='0'
          width='99%'
          class='<c:out value="${colorStr}" />'>
          <tr>
            <td colspan='2'><img
              src='<c:url value="/images/blank.gif"/>'
              height='15'
              width='1' /></td>
          </tr>

          <tr
            class='left'
            bgcolor='${teamHeaderColor[team.teamNumber] }'>
            <td width='25%'>&nbsp;&nbsp;${team.awardGroup }&nbsp;&nbsp;
            </td>
            <td class='right'>Team&nbsp;#:&nbsp;${team.teamNumber}&nbsp;&nbsp;
            </td>
          </tr>
          <tr class='left'>
            <td colspan='2'>&nbsp;&nbsp;${team.organization }</td>
          </tr>
          <tr class='left'>
            <td colspan='2'>&nbsp;&nbsp;${team.teamName}</td>
          </tr>
          <tr>
            <td colspan='2'>
              <hr
                style='color: #ffffff;'
                width='96%' />
            </td>
          </tr>

          <tr>
            <td colspan='2'>
              <table
                border='0'
                cellpadding='1'
                cellspacing='0'>
                <tr class='center'>
                  <td><img
                    src='<c:url value="/images/blank.gif"/>'
                    height='1'
                    width='60' /></td>
                  <td>Run #</td>
                  <td><img
                    src='<c:url value="/images/blank.gif"/>'
                    width='20'
                    height='1' /></td>
                  <td>Score</td>
                </tr>

                <c:forEach
                  items="${teamScores }"
                  var="score">
                  <tr class='right'>
                    <td><img
                      src='<c:url value="/images/blank.gif"/>'
                      height='1'
                      width='60' /></td>
                    <td>${score.runNumber }</td>
                    <td><img
                      src='<c:url value="/images/blank.gif"/>'
                      width='20'
                      height='1' /></td>
                    <td>${score.scoreString }</td>
                  </tr>

                </c:forEach>
                <!--  foreach score -->
              </table> <!-- scores table -->
            </td>
          </tr>

          <c:choose>
            <c:when test="${not empty sponsorLogos and (teamIndex mod teamsBetweenLogos) == 1}">
              <tr style='background-color: white'>
                <td
                  width='50%'
                  style='vertical-align: middle; color: black'
                  class="right">This tournament sponsored by:</td>

                <td
                  width='50%'
                  style='vertical-align: middle; padding: 3px'
                  class="left"><img
                  src='../${sponsorLogos[(teamIndex / teamsBetweenLogos) mod fn:length(sponsorLogos)] }' />

                </td>
              </tr>
            </c:when>
            <c:otherwise>
              <tr>
                <td colspan='2'><img
                  src='<c:url value="/images/blank.gif"/>'
                  width='1'
                  height='15' /></td>
              </tr>
            </c:otherwise>
          </c:choose>

        </table>
        <!-- team table -->

        <c:choose>
          <c:when test="${'A' == colorStr}">
            <c:set
              var="colorStr"
              value="B" />
          </c:when>
          <c:otherwise>
            <c:set
              var="colorStr"
              value="A" />
          </c:otherwise>
        </c:choose>

      </c:forEach>
      <!-- foreach team -->

    </c:when>
    <c:when test="${not empty sponsorLogos}">
      <table
        class="center"
        style='background-color: white; color: black'
        width="99%">
        <tr>
          <th>This tournament sponsored by</th>
        </tr>
        <c:forEach
          items="${sponsorLogos }"
          var="logo">
          <tr>
            <td style='vertical-align: middle; padding: 3px'><img
              src='../${logo}' /></td>
          </tr>
        </c:forEach>
      </table>
    </c:when>
  </c:choose>

  <table
    border='0'
    cellpadding='0'
    cellspacing='0'
    width='99%'>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
  </table>


  <span id="bottom">&nbsp;</span>

</body>
</html>

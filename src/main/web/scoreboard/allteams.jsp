<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.scoreboard.AllTeams.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>All Teams scoreboard</title>
<link rel='stylesheet' type='text/css' href='../style/base.css' />
<link rel='stylesheet' type='text/css' href='score_style.css' />

<script type="text/javascript"
    src='<c:url value="/js/fll-functions.js"/>'></script>
<script type="text/javascript" src="set-font-size.js"></script>

<style>
TABLE.A {
    background-color: #000080
}

TABLE.B {
    background-color: #0000d0
}
</style>

<script type="text/javascript">
  let prevScrollTimestamp = 0;
  const secondsBetweenScrolls = parseFloat("${scrollRate}");
  // using 1 doesn't work with chromebooks
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
      // show the last scores for a bit and then reload
      setTimeout(reload, 3000);
    }
  }
</script>

<c:if test="${param.allTeamsScroll}">
    <c:choose>
        <c:when test="${fn:length(teamsWithScores) gt 0}">
            <script type="text/javascript">
                          document.addEventListener("DOMContentLoaded",
                              function() {
                                requestAnimationFrame(scrollDown);
                              });
                        </script>
        </c:when>
        <c:otherwise>
            <script type="text/javascript">
                          document.addEventListener("DOMContentLoaded",
                              function() {
                                // reload after 5 seconds
                                setTimeout(reload, 5000);
                              });
                        </script>
        </c:otherwise>
    </c:choose>
</c:if>


</head>

<body class='scoreboard'>
    <br />
    <br />
    <br />
    <br />
    <br />
    <br />
    <br />
    <br />

    <c:set var="colorStr" value="A" />


    <c:choose>
        <c:when test="${fn:length(teamsWithScores) gt 0}">
            <c:forEach items="${teamsWithScores }" var="team"
                varStatus="loopStatus">
                <c:set var="teamScores"
                    value="${scores[team.teamNumber] }" />
                <c:set var="teamIndex" value="${loopStatus.index }" />

                <table border='0' cellpadding='0' cellspacing='0'
                    width='99%' class='<c:out value="${colorStr}" />'>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>'
                                height='15' width='1' />
                        </td>
                    </tr>

                    <tr class='left'
                        bgcolor='${teamHeaderColor[team.teamNumber] }'>
                        <c:choose>
                            <c:when
                                test="${team.awardGroup != team.judgingGroup }">
                                <td width='25%'>&nbsp;&nbsp;${team.awardGroup }&nbsp;&nbsp;(${team.judgingGroup })</td>
                            </c:when>
                            <c:otherwise>
                                <td width='25%'>&nbsp;&nbsp;${team.awardGroup }&nbsp;&nbsp;</td>
                            </c:otherwise>
                        </c:choose>
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
                            <hr style='color: #ffffff;' width='96%' />
                        </td>
                    </tr>

                    <tr>
                        <td colspan='2'>
                            <table border='0' cellpadding='1'
                                cellspacing='0'>
                                <tr class='center'>
                                    <td>
                                        <img
                                            src='<c:url value="/images/blank.gif"/>'
                                            height='1' width='60' />
                                    </td>
                                    <td>Run #</td>
                                    <td>
                                        <img
                                            src='<c:url value="/images/blank.gif"/>'
                                            width='20' height='1' />
                                    </td>
                                    <td>Score</td>
                                </tr>

                                <c:forEach items="${teamScores }"
                                    var="score">
                                    <tr class='right'>
                                        <td>
                                            <img
                                                src='<c:url value="/images/blank.gif"/>'
                                                height='1' width='60' />
                                        </td>
                                        <td>${score.runNumber }</td>
                                        <td>
                                            <img
                                                src='<c:url value="/images/blank.gif"/>'
                                                width='20' height='1' />
                                        </td>
                                        <td>${score.scoreString }</td>
                                    </tr>

                                </c:forEach>
                                <!--  foreach score -->
                            </table>
                            <!-- scores table -->
                        </td>
                    </tr>

                    <c:choose>
                        <c:when
                            test="${not empty sponsorLogos and (teamIndex mod teamsBetweenLogos) == 1}">
                            <tr style='background-color: white'>
                                <td width='50%'
                                    style='vertical-align: middle; color: black'
                                    class="right">This tournament
                                    sponsored by:</td>

                                <td width='50%'
                                    style='vertical-align: middle; padding: 3px'
                                    class="left">
                                    <img
                                        src='../${sponsorLogos[(teamIndex / teamsBetweenLogos) mod fn:length(sponsorLogos)] }' />

                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan='2'>
                                    <img
                                        src='<c:url value="/images/blank.gif"/>'
                                        width='1' height='15' />
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>

                </table>
                <!-- team table -->


                <%--alternate background colors --%>
                <c:choose>
                    <c:when test="${'A' == colorStr}">
                        <c:set var="colorStr" value="B" />
                    </c:when>
                    <c:otherwise>
                        <c:set var="colorStr" value="A" />
                    </c:otherwise>
                </c:choose>

            </c:forEach>
            <!-- foreach team -->

        </c:when>
        <c:when test="${not empty sponsorLogos}">
            <!-- no teams with scores - display all sponsor logos -->
            <table class="center"
                style='background-color: white; color: black'
                width="99%">
                <tr>
                    <th>This tournament sponsored by</th>
                </tr>
                <c:forEach items="${sponsorLogos }" var="logo">
                    <tr>
                        <td style='vertical-align: middle; padding: 3px'>
                            <img src='../${logo}' />
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:when>
    </c:choose>

    <%-- blank space at the bottom --%>
    <table border='0' cellpadding='0' cellspacing='0' width='99%'>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
        <tr>
            <td colspan='2'>
                <img src='<c:url value="/images/blank.gif"/>' width='1'
                    height='15' />
            </td>
        </tr>
    </table>


    <span id="bottom">&nbsp;</span>

</body>
</html>

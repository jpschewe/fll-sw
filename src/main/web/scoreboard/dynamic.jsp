<!DOCTYPE html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.scoreboard.Title.populateContext(application, session, pageContext);
fll.web.scoreboard.AllTeams.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Scoreboard</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<link rel='stylesheet' type='text/css' href='score_style.css' />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript" src="dynamic.js"></script>

<script type="text/javascript" src="set-font-size.js"></script>


</head>
<body class='scoreboard'>

    <div id='left'>
        <div id='title' class='center bold'>
            ${awardGroupTitle}
            <br />
            ${ScorePageText }
        </div>

        <div id='all_teams'>
            <c:forEach items="${allTeams}" var="team"
                varStatus="loopStatus">
                <c:set var="teamIndex" value="${loopStatus.index }" />

                <!--  start team table -->
                <table border='0' cellpadding='0' cellspacing='0'
                    width='99%' id='all_teams_${team.teamNumber}'
                    class='${colorStr}'>
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

                                <!-- FIXME need to figure out how to know the number of runs -->
                                <!-- 
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
                             -->
                            </table>
                            <!-- scores table -->
                        </td>
                    </tr>

                    <!-- sponsor section for team -->
                    <c:choose>
                        <c:when
                            test="${not empty sponsorLogos and (teamIndex mod teamsBetweenLogos) == 1}">
                            <tr style='background-color: white'>
                                <td width='50%'
                                    style='vertical-align: middle; color: black'
                                    class="right sponsor_title">This
                                    tournament sponsored by:</td>

                                <td width='50%'
                                    style='vertical-align: middle; padding: 3px'
                                    class="left sponsor_logo">
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
                                        width='1' height='15'
                                        class='all_teams_blank' />
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                    <!-- end sponsor section for team -->

                </table>
                <!-- end team table -->


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
        </div>
        <!-- end all teams -->
    </div>
    <!-- end left -->

    <div id='right'>
        <div id='top_scores'>top scores</div>
        <!-- end top scores -->

        <div id='most_recent'>
            <table id="most_recent_table">
            </table>
        </div>
        <!-- end most recent -->
    </div>
    <!-- end right -->

</body>
</html>

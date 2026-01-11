<!DOCTYPE html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.scoreboard.Dynamic.populateContext(request, application, pageContext);
%>

<html>
<head>
<title>Scoreboard${additionalTitle}</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/big_screen.css'/>" />

<link rel='stylesheet' type='text/css' href='dynamic.css' />

<script type="text/javascript">
  var secondsBetweenScrolls = parseFloat("${scrollRate}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree
</script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript" src="dynamic.js"></script>

<script type="text/javascript" src="set-font-size.js"></script>

<script type="text/javascript">
const awardGroupColorsRaw = JSON.parse('${awardGroupColors}');
for (const [awardGroup, color] of Object.entries(awardGroupColorsRaw)) {
  awardGroupColors.set(awardGroup, color);
}

const divisionFlipRate = parseInt("${divisionFlipRate}");
const layout = "${layout}"
const displayUuid = "${param.display_uuid}";
const REGISTER_MESSAGE_TYPE = "${REGISTER_MESSAGE_TYPE}";
const UPDATE_MESSAGE_TYPE = "${UPDATE_MESSAGE_TYPE}";
const DELETE_MESSAGE_TYPE = "${DELETE_MESSAGE_TYPE}";
const RELOAD_MESSAGE_TYPE = "${RELOAD_MESSAGE_TYPE}";
const SCORE_TEXT_MESSAGE_TYPE = "${SCORE_TEXT_MESSAGE_TYPE}";

const INITIAL_SCORE_PAGE_TEXT = "${ScorePageText}"
</script>

</head>
<body class='scoreboard'>
    <div id='container'>

        <div id='left'>
            <div id='title' class='center bold'>
                <div id='title-top'>
                    <span id='clock'></span>

                    <span id='awardGroupTitle'>
                        ${awardGroupTitle} </span>
                </div>
                <div id='title-bottom'>
                    <span id="scorePageText"></span>
                </div>
            </div>

            <div id='all_teams'>
                <div id="all_teams_top"></div>

                <%-- blank space at the top --%>
                <table class='spacer'>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                </table>

                <%-- Initial value for colorStr --%>
                <c:set var="colorStr" value="A" />

                <c:forEach items="${allTeams}" var="team"
                    varStatus="loopStatus">
                    <c:set var="teamIndex" value="${loopStatus.index }" />

                    <!--  start team -->
                    <table class="team ${colorStr} fll-sw-ui-inactive"
                        id='all_teams_${team.teamNumber}'>
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

                        <!-- team scores -->
                        <tr>
                            <td colspan='2'>
                                <table
                                    id="all_teams_${team.teamNumber}_scores"
                                    class='runs'>
                                </table>
                            </td>
                        </tr>
                        <!-- end team scores -->

                        <!-- sponsor section for team -->
                        <c:choose>
                            <c:when
                                test="${not empty sponsorLogos and (teamIndex mod teamsBetweenLogos) == 1}">
                                <tr class='sponsor'>
                                    <td class="right sponsor_title">This
                                        season sponsored by:&nbsp;</td>

                                    <td class="left sponsor_logo">
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
                                            class='all_teams_blank' />
                                    </td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                        <!-- end sponsor section for team -->

                    </table>
                    <!-- end team -->


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

                <%-- blank space at the bottom --%>
                <table class='spacer'>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                </table>

                <div id="all_teams_bottom">&nbsp;</div>

                <!-- bottom space for the scrolling to work correctly -->
                <table class='spacer'>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                    <tr>
                        <td colspan='2'>
                            <img
                                src='<c:url value="/images/blank.gif"/>' />
                        </td>
                    </tr>
                </table>
            </div>
            <!-- end all teams -->
        </div>
        <!-- end left -->

        <div id='right'>
            <div id='top_scores'>
                <table id="top_scores_table"></table>
            </div>
            <!-- end top scores -->

            <div id='most_recent'>
                <table id="most_recent_table">
                </table>
            </div>
            <!-- end most recent -->
        </div>
        <!-- end right -->
    </div>
</body>
</html>

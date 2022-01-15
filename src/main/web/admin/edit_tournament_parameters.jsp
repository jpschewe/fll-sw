<%@page import="fll.web.admin.GatherTournamentParameterInformation"%>
<%@page import="fll.web.admin.GatherParameterInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
GatherTournamentParameterInformation.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Parameters</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<!-- functions to displaying and hiding help -->
<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>

</head>

<body>

    <div class='content'>

        <h1>Edit Tournament Parameters</h1>

        <div class='status-message'>${message}</div>
        <%-- clear out the message, so that we don't see it again --%>
        <c:remove var="message" />

        <div>Editing parameters for ${tournament.description}</div>

        <form name='edit_tournament_parameters'
            id='edit_tournament_parameters'
            action='StoreTournamentParameters' method='POST'>

            <!-- num seeding rounds -->
            <div>
                <b>Number of Regular Match Play Rounds</b>
                <c:choose>
                    <c:when test="${numSeedingRoundsDisabled}">
                        <i>This parameter cannot be changed once any
                            performance scores have been entered.</i>
                        <input type="number" name="seeding_rounds"
                            id="seeding_rounds"
                            value="${numSeedingRounds}"
                            required readonly />
                    </c:when>
                    <c:otherwise>
                        <a
                            href='javascript:display("SeedingRoundsHelp")'>[help]</a>
                        <div id='SeedingRoundsHelp' class='help'
                            style='display: none'>
                            This parameter specifies the number of
                            rounds in regular match play. Regular match
                            play rounds are used for the performance
                            score in the final report and are typically
                            used to rank teams for the initial head to
                            head round. <a
                                href='javascript:hide("SeedingRoundsHelp")'>[hide]</a>
                        </div>

                        <input type="number" name="seeding_rounds"
                            id="seeding_rounds"
                            value="${numSeedingRounds}"
                            min="0" required />
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- num practice rounds -->
            <div>
                <b>Number of Practice Rounds</b>
                <c:choose>
                    <c:when test="${numSeedingRoundsDisabled}">
                        <i>This parameter cannot be changed once any
                            performance scores have been entered.</i>
                        <input type="number" name="practice_rounds"
                            id="practice_rounds"
                            value="${numPracticeRounds}"
                            required readonly />
                    </c:when>
                    <c:otherwise>
                        <input type="number" name="practice_rounds"
                            id="practice_rounds"
                            value="${numPracticeRounds}"
                            min="0" required />
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- running head to head -->
            <div>
                <b>Running head to head</b>
                <c:choose>
                    <c:when test="${runningHeadToHeadDisabled }">
                        <i>This parameter cannot be changed once any
                            performance scores beyond the number of
                            regular match play rounds have been entered
                            or there is an initialized playoff bracket.</i>
                        <input type="checkbox"
                            name="running_head_to_head"
                            id="running_head_to_head" readonly ${runningHeadToHeadChecked }/>
                    </c:when>
                    <c:otherwise>
                        <a
                            href='javascript:display("RunningHeadToHeadHelp")'>[help]</a>
                        <div id='RunningHeadToHeadHelp' class='help'
                            style='display: none'>
                            When this parameter is unset, then there are
                            no head to head rounds. Instead there are
                            extra performance runs that don't count for
                            the final reports. <a
                                href='javascript:hide("RunningHeadToHeadHelp")'>[hide]</a>
                        </div>
                        <input type="checkbox"
                            name="running_head_to_head"
                            id="running_head_to_head"
                            ${runningHeadToHeadChecked } />
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- performance advancement percentage -->
            <div>
                <b>Performance Advancement Percentage</b>
                <a
                    href='javascript:display("PerformanceAdvancementPercentageHelp")'>[help]</a>
                <div id='PerformanceAdvancementPercentageHelp'
                    class='help' style='display: none'>
                    Teams should have a performance score in the top X%
                    of all performance scores to be allowed to advance
                    to the next tournament. X is a positive integer. <a
                        href='javascript:hide("PerformanceAdvancementPercentageHelp")'>[hide]</a>
                </div>

                <input type="number"
                    name="performance_advancement_percentage"
                    id="performance_advancement_percentage"
                    value="${performanceAdvancementPercentage}"
                    min="1" required />

            </div>

            <input type="submit" id='submit_data' name='submit_data'
                value='Submit' />

        </form>
    </div>
</body>
</html>

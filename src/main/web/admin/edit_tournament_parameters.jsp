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
                    value="${performanceAdvancementPercentage}" min="0"
                    required />

            </div>

            <!-- pit sign top text -->
            <div>
                <b>Pit sign top text</b>
                <a href='javascript:display("PitSignTopTextHelp")'>[help]</a>
                <div id='PitSignTopTextHelp' class='help'
                    style='display: none'>
                    Text displayed above the schedule on the pit signs.
                    <a href='javascript:hide("PitSignTopTextHelp")'>[hide]</a>
                </div>

                <textarea name="pit_sign_top_text"
                    id="pit_sign_top_text" rows="5" cols="80">${pitSignTopText}</textarea>
            </div>

            <!-- pit sign bottom text -->
            <div>
                <b>Pit sign bottom text</b>
                <a href='javascript:display("PitSignBottomTextHelp")'>[help]</a>
                <div id='PitSignBottomTextHelp' class='help'
                    style='display: none'>
                    Text displayed below the schedule on the pit signs.
                    <a href='javascript:hide("PitSignBottomTextHelp")'>[hide]</a>
                </div>

                <textarea name="pit_sign_bottom_text"
                    id="pit_sign_bottom_text" rows="5" cols="80">${pitSignBottomText}</textarea>
            </div>

            <input type="submit" id='submit_data' name='submit_data'
                value='Submit' />

        </form>
    </div>
</body>
</html>

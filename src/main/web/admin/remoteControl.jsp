<%@page import="fll.web.display.DisplayInfo"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.RemoteControl.populateContext(application, pageContext);
%>

<html>
<head>
<title>Display Controller</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript">
  "use-strict";
  var numPlayoffRounds = parseInt("${numPlayoffRounds}"); // could be here directly as an intger, but the JSTL and auto-formatting don't agree
  var divisions = [];
  var displayUuids = [];
</script>

<c:forEach items="${divisions}" var="division">
    <script type="text/javascript">
          "use-strict";
          divisions.push("${division}");
        </script>
</c:forEach>

<c:forEach items="${displayInformation}" var="displayInfo">
    <script type="text/javascript">
          "use-strict";
          displayUuids.push("${displayInfo.uuid}");
        </script>
</c:forEach>

<script type="text/javascript" src="remoteControl.js"></script>

</head>
<body>

    <h1>Display Controller</h1>

    <p>This page is used to control what page is currently visible
        on the display screen. Note that it takes some time for the
        display to change, up to 2 minutes.</p>

    <p>The last seen time for each display is only updated when the
        form on this page is submitted. The timezone matches that of the
        server.</p>


    <%-- NOTE: The values of the radio buttons need to match up with the strings in DisplayInfo --%>
    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <form name='remote' action='RemoteControlPost' method='post'>

        <table border='1'>
            <tr>
                <td>&nbsp;</td>
                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <th>
                        <c:choose>
                            <c:when test="${displayInfo.defaultDisplay}">          
          ${displayInfo.name}
          </c:when>
                            <c:otherwise>
          ${displayInfo.name}-seen@${displayInfo.lastSeen}
          </c:otherwise>
                        </c:choose>
                    </th>
                </c:forEach>
            </tr>

            <tr>
                <th>Delete Named Display. If the display is still
                    active it will be reset to following the default
                    display.</th>
                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <!-- uuid: ${displayInfo.uuid} name: ${displayInfo.name} -->
                        <!-- default: ${displayInfo.defaultDisplay} -->
                        <!-- form prefix: ${displayInfo.formParamPrefix} -->
                        <!-- followDefault: ${displayInfo.followDefault} -->
                        <!-- welcome: ${displayInfo.welcome} -->
                        <!-- scoreboard: ${displayInfo.scoreboard} -->
                        <!-- remotePageFormParamName: ${displayInfo.remotePageFormParamName} -->
                        <!-- headToHead: ${displayInfo.headToHead} -->
                        <!-- finalistTeams: ${displayInfo.finalistTeams} -->
                        <!-- slideshow: ${displayInfo.slideshow} -->
                        <!-- special: ${displayInfo.special} -->
                        <c:choose>
                            <c:when test="${displayInfo.defaultDisplay}">
          &nbsp;
          </c:when>
                            <c:otherwise>
                                <input type="checkbox"
                                    name="${displayInfo.deleteFormParamName}" />
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:forEach>
            </tr>

            <tr>
                <th>Specify the award groups to show on this
                    display. None checked is the same as all checked</th>
                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <!-- choose award groups -->
                        <c:forEach items="${allAwardGroups}"
                            var="awardGroup" varStatus="agIterStatus">
                            <c:choose>
                                <c:when
                                    test="${ displayInfo.determineScoreboardAwardGroups(allAwardGroups).contains(awardGroup) }">
                                    <input type="checkbox"
                                        name="${displayInfo.awardGroupsFormParamName}"
                                        id="${displayInfo.awardGroupsFormParamName}${agIterStatus.index}"
                                        value="${awardGroup}" checked />
                                </c:when>
                                <c:otherwise>
                                    <input type="checkbox"
                                        name="${displayInfo.awardGroupsFormParamName}"
                                        id="${displayInfo.awardGroupsFormParamName}${agIterStatus.index}"
                                        value="${awardGroup}" />
                                </c:otherwise>
                            </c:choose>
                            <label
                                for="${displayInfo.awardGroupsFormParamName}${agIterStatus.index}">${awardGroup}</label>
                            <br />
                        </c:forEach>
                    </td>
                </c:forEach>
            </tr>

            <tr>
                <th>Follow Default</th>
                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <c:choose>
                            <c:when test="${displayInfo.defaultDisplay}">
              &nbsp;
            </c:when>
                            <c:otherwise>
                                <c:choose>
                                    <c:when
                                        test="${displayInfo.followDefault}">
                                        <input type='radio'
                                            name="${displayInfo.remotePageFormParamName}"
                                            value='default' checked />
                                    </c:when>
                                    <c:otherwise>
                                        <input type='radio'
                                            name="${displayInfo.remotePageFormParamName}"
                                            value='default' />
                                    </c:otherwise>
                                </c:choose>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:forEach>
            </tr>

            <tr>
                <th>Welcome</th>
                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <c:choose>
                            <c:when test="${displayInfo.welcome}">
                                <input type='radio' id='welcome'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.WELCOME_REMOTE_PAGE%>'
                                    checked />
                            </c:when>
                            <c:otherwise>
                                <input type='radio' id='welcome'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.WELCOME_REMOTE_PAGE%>' />
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:forEach>
            </tr>
            <!-- welcome -->

            <tr>
                <th>Scoreboard</th>
                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <c:choose>
                            <c:when test="${displayInfo.scoreboard}">
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.SCOREBOARD_REMOTE_PAGE%>'
                                    checked />
                            </c:when>
                            <c:otherwise>
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.SCOREBOARD_REMOTE_PAGE%>' />
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:forEach>
            </tr>
            <!--  Scoreboard -->

            <tr>
                <th>Scoreboard - Most Recent and All Teams Scores</th>
                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <c:choose>
                            <c:when
                                test="${displayInfo.scoreboardMostRecentAllTeams}">
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.SCOREBOARD_MOST_RECENT_ALL_TEAMS_REMOTE_PAGE%>'
                                    checked />
                            </c:when>
                            <c:otherwise>
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.SCOREBOARD_MOST_RECENT_ALL_TEAMS_REMOTE_PAGE%>' />
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:forEach>
            </tr>
            <!--  Scoreboard - most recent and all teams -->

            <tr>
                <th>Head to Head</th>

                <c:forEach items="${displayInformation}"
                    var="displayInfo">

                    <td>
                        <c:set var="brackets"
                            value="${displayInfo.brackets}" />
                        <input type='hidden'
                            id="${displayInfo.formParamPrefix}numBrackets"
                            name="${displayInfo.head2HeadNumBracketsFormParamName}"
                            value="${fn:length(brackets)}" />
                        <c:choose>
                            <c:when test="${displayInfo.headToHead}">
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.HEAD_TO_HEAD_REMOTE_PAGE%>'
                                    checked />
                            </c:when>
                            <c:otherwise>
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.HEAD_TO_HEAD_REMOTE_PAGE%>' />
                            </c:otherwise>
                        </c:choose>

                        <div
                            id="${displayInfo.formParamPrefix}bracket_selection">

                            <c:forEach items="${brackets}"
                                var="bracketInfo">

                                <div
                                    id="${displayInfo.formParamPrefix}bracket_${bracketInfo.index}">
                                    Bracket:
                                    <select
                                        name="${bracketInfo.head2HeadBracketFormParamName}">

                                        <c:forEach items="${divisions}"
                                            var="division">
                                            <c:choose>
                                                <c:when
                                                    test="${division == bracketInfo.bracket}">
                                                    <option
                                                        value="${division}"
                                                        selected>${division}</option>
                                                </c:when>
                                                <c:otherwise>
                                                    <option
                                                        value="${division}">${division}</option>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>

                                    </select>
                                    <br />
                                    Round:
                                    <select
                                        name="${bracketInfo.head2HeadFirstRoundFormParamName}">

                                        <c:forEach begin="1"
                                            end="${numPlayoffRounds}"
                                            var="numRounds">
                                            <c:choose>
                                                <c:when
                                                    test="${numRounds == bracketInfo.firstRound}">
                                                    <option
                                                        value="${numRounds}"
                                                        selected>${numRounds}</option>
                                                </c:when>
                                                <c:otherwise>
                                                    <option
                                                        value="${numRounds}">${numRounds}</option>
                                                </c:otherwise>
                                            </c:choose>
                                        </c:forEach>

                                    </select>
                                </div>

                                <hr />
                            </c:forEach>

                        </div>
                        <!-- end bracket selection -->

                        <button type='button'
                            id='${displayInfo.formParamPrefix}add_bracket'>Add
                            Bracket</button>
                        <br />
                        <button type='button'
                            id='${displayInfo.formParamPrefix}remove_bracket'>Remove
                            Bracket</button>
                    </td>

                </c:forEach>

            </tr>
            <!--  Head to Head -->

            <tr>
                <th>Finalist Teams</th>

                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <c:choose>
                            <c:when test="${displayInfo.finalistTeams}">
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.FINALIST_TEAMS_REMOTE_PAGE%>'
                                    checked />
                            </c:when>
                            <c:otherwise>
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.FINALIST_TEAMS_REMOTE_PAGE%>' />
                            </c:otherwise>
                        </c:choose>
                    </td>
                </c:forEach>
            </tr>
            <!--  Finalist teams -->

            <tr>
                <th>
                    Slide show
                    <br />
                    Seconds to show a slide:
                    <input type='text' name='slideInterval'
                        value="${slideShowInterval}" size='3' />
                </th>

                <c:if test="${not empty displayInformation}">
                    <c:forEach items="${displayInformation}"
                        var="displayInfo">
                        <td>
                            <c:choose>
                                <c:when test="${displayInfo.slideshow}">
                                    <input type='radio'
                                        name="${displayInfo.remotePageFormParamName}"
                                        value='<%=DisplayInfo.SLIDESHOW_REMOTE_PAGE%>'
                                        checked />
                                </c:when>
                                <c:otherwise>
                                    <input type='radio'
                                        name="${displayInfo.remotePageFormParamName}"
                                        value='<%=DisplayInfo.SLIDESHOW_REMOTE_PAGE%>' />
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </c:forEach>
                </c:if>

            </tr>
            <!--  Slideshow -->

            <tr>
                <th>Specify page relative to /custom</th>

                <c:forEach items="${displayInformation}"
                    var="displayInfo">
                    <td>
                        <c:choose>
                            <c:when test="${displayInfo.special}">
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.SPECIAL_REMOTE_PAGE%>'
                                    checked />
                            </c:when>
                            <c:otherwise>
                                <input type='radio'
                                    name="${displayInfo.remotePageFormParamName}"
                                    value='<%=DisplayInfo.SPECIAL_REMOTE_PAGE%>' />
                            </c:otherwise>
                        </c:choose>
                        <input type='text'
                            name='${displayInfo.specialUrlFormParamName}'
                            value="${displayInfo.specialUrl}">
                    </td>
                </c:forEach>

            </tr>
            <!-- specify page -->

        </table>

        <input type='submit' name='submit_data' value='Submit' />

    </form>

    <form action="<c:url value='/admin/ChangeScorePageText'/>"
        method='post'>
        <div>
            Scoring display text:
            <input type='text' name='ScorePageText'
                value='<c:out value="${ScorePageText}"/>'>
            <input type='submit' value='Change text'>
        </div>
        <div>This text is displayed on the various big screen
            display pages. There is only 1 or 2 lines of space
            available, so keep it short. This can be used to notify
            participants and spectators of when the next break will be
            over.</div>
    </form>


    <h2>Troubleshooting</h2>

    <p>If you have a display that is stale. Look at the seen @ time
        in the table. If it's more than a few minutes in the past (note
        the time on the server computer). Then click the delete checkbox
        and then submit. This will remove the display name from the list
        and it won't come back until the display computer checks in with
        the server again; usually about every 30 seconds.</p>

    <p>If a display isn't updating, even after 2 minutes; walk over
        to the display computer and refresh the page with F5 or ctrl+r.
        Also use Alt-Tab (or Atl-` on some computers) to switch to the
        other browser window that has the name on it. Refresh that page
        also. At this point the display should be behaving. If it isn't,
        then close the browser on the display computer and start it
        again.</p>

    <p>If you are seeing extra columns in the table above. Click the
        delete checkbox and then submit. Sometimes you might want to
        delete all of them. Don't worry about deleting an active
        display, it will still come back when it checks in with the
        server again.</p>

</body>
</html>

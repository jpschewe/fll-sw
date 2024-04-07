<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.TeamColumnSelection.populateContext(session, pageContext);
%>

<!-- query string <c:out value="${request.queryString}"/> -->

<html>
<head>
<title>Team Column Selection</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="teamColumnSelection.js"></script>

<script type='text/javascript'>
  "use strict";

  const TEAM_NUMBER_HEADER = "${TEAM_NUMBER_HEADER}";
  const TEAM_NAME_HEADER = "${TEAM_NAME_HEADER}";
  const ORGANIZATION_HEADER = "${ORGANIZATION_HEADER}";
  const AWARD_GROUP_HEADER = "${AWARD_GROUP_HEADER}";
  const JUDGE_GROUP_HEADER = "${JUDGE_GROUP_HEADER}";
  const WAVE_HEADER = "${WAVE_HEADER}";
</script>

</head>

<body>
    <h1>Team Column Selection</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <p>Do not use the forward and back buttons! Use the supplied
        links/buttons.</p>

    <p>
        Match the software information on the left with the names of the
        columns from your data file on the right. The column names are
        the first row of your data file.
        <b>If a number is expected and you specify a column with
            text that doesn't convert to a number an error will be
            printed specifying the invalid value.</b>
        You can select the same column for multiple pieces of data.
    </p>

    <%@ include file="/WEB-INF/jspf/sanitizeRules.jspf"%>

    <c:if test="errorMessage">
        <p class='error'>${errorMessage}</p>
    </c:if>

    <form action="verifyTeams.jsp" method="POST" name="verifyTeams">
        <table border='1'>

            <%-- Note all form elements need to match the names of the columns in the database --%>
            <tr>
                <th>Software information</th>
                <th>Datatype - size</th>
                <th>Column from datafile</th>
            </tr>

            <tr bgcolor='yellow'>
                <td>TeamNumber</td>
                <td>Number</td>
                <td>
                    <select id='teamNumber' name='TeamNumber'>
                        ${columnSelectOptions}
                    </select>
                </td>
            </tr>

            <tr>
                <td>Team Name</td>
                <td>Text - 255 characters</td>
                <td>
                    <select id='teamName' name='TeamName'>
                        <option value='' selected>None</option>
                        ${columnSelectOptions}
                    </select>
                </td>
            </tr>

            <tr>
                <td>Organization</td>
                <td>Text - 255 characters</td>
                <td>
                    <select id='organization' name='Organization'>
                        <option value='' selected>None</option>
                        ${columnSelectOptions}
                    </select>
                </td>
            </tr>

            <tr>
                <td>
                    Initial Tournament - if specified the Award Group
                    and Judging Group
                    <i>must</i>
                    be specified.
                </td>
                <td>Text - 255 characters</td>
                <td>
                    <select name='tournament'>
                        <option value='' selected>None</option>
                        ${columnSelectOptions}
                    </select>
                </td>
            </tr>

            <tr>
                <td>Award Group</td>
                <td>Text - 32 characters</td>
                <td>
                    <select id='awardGroup' name='event_division'>
                        <option value='' selected>None</option>
                        ${columnSelectOptions}
                    </select>
                </td>
            </tr>

            <tr>
                <td>Judging Group</td>
                <td>Text - 32 characters</td>
                <td>
                    <select id='judgingGroup' name='judging_station'>
                        <option value='' selected>None</option>
                        ${columnSelectOptions}
                    </select>
                </td>
            </tr>

            <tr>
                <td>Wave</td>
                <td>Text - 32 characters</td>
                <td>
                    <select id='wave' name='wave'>
                        <option value='' selected>None</option>
                        ${columnSelectOptions}
                    </select>
                </td>
            </tr>


            <tr>
                <td colspan='3'>
                    <input type='submit' id='next' value='Next'>
                </td>
            </tr>

        </table>
    </form>


</body>
</html>

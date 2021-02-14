<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Add missing teams (Upload Schedule)</title>
</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="promptCreateMissingTeams" action="AddScheduleTeams">

        <p>The following teams are in the schedule and not in the
            destination database. OK to add teams?</p>

        <table border='1'>
            <tr>
                <th>Team Number</th>
                <th>Team Name</th>
            </tr>
            <c:forEach items="${uploadScheduleData.missingTeams}"
                var="team">
                <tr>
                    <td>${team.teamNumber}</td>
                    <td>${team.teamName}</td>
                </tr>
            </c:forEach>
        </table>

        <input name='submit_data_yes' type='submit' value="Add teams" />
    </form>

    <form name="promptCreateMissingTeams" action="CheckViolations">
        <input name='submit_data_no' type='submit'
            value="Do NOT Add teams" />
    </form>


</body>
</html>

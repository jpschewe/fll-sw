<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.CommitAwardGroups.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Edit Award Groups</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Edit Award Groups</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>This page allows you to assign teams to award groups. Below
        are the teams at the current tournament and what award group
        they are in. You can either select one of the existing award
        groups or select the text field and enter a new award group.</p>

    <form name='edit_event_divisions' action='CommitAwardGroups'
        method='post'>
        <table id='data' border='1'>
            <tr>
                <th>Team Number</th>
                <th>Team Name</th>
                <th>Award Group</th>
            </tr>
            <c:forEach items="${teams}" var="team">
                <tr>
                    <td>${team.teamNumber}</td>
                    <td>${team.teamName}</td>

                    <td>
                        <c:forEach items="${divisions}" var="division">
                            <c:choose>
                                <c:when
                                    test="${team.awardGroup == division}">
                                    <input type='radio'
                                        name='${team.teamNumber}'
                                        value='${division}'
                                        id='${team.teamNumber}_${division}'
                                        checked />
                                </c:when>
                                <c:otherwise>
                                    <input type='radio'
                                        name='${team.teamNumber}'
                                        value='${division}'
                                        id='${team.teamNumber}_${division}' />
                                </c:otherwise>
                            </c:choose>
                            <label for='${team.teamNumber}_${division}'>${division}</label>
                        </c:forEach>
                        <input type='radio' name='${team.teamNumber}'
                            value='text' />
                        <input type='text'
                            name='${team.teamNumber}_text'
                            id='${team.teamNumber}_text' />
                    </td>
                </tr>
            </c:forEach>
        </table>

        <input type='submit' name='submit_data' value='Commit' />
        <input type='submit' name='submit_data' value='Cancel' />
    </form>


</body>
</html>

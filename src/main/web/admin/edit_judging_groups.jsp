<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.CommitJudgingGroups.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Edit Judging Groups</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Edit Judging Groups</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>This page allows you to assign teams to judging groups. Below
        are the teams at the current tournament and what judging group
        they are in. You can either select one of the existing judging
        groups or select the text field and enter a new award group.
    <p>
    <p>
        <b>If the judging group is changed for a team that already
            has subjective scores entered, those scores will be deleted.</b>
    </p>

    <form name='edit_judging_groups' action='CommitJudgingGroups'
        method='post'>
        <table id='data' border='1'>
            <tr>
                <th>Team Number</th>
                <th>Team Name</th>
                <th>Judging Group</th>
            </tr>
            <c:forEach items="${teams}" var="team">
                <tr>
                    <td>${team.teamNumber}</td>
                    <td>${team.teamName}</td>

                    <td>
                        <c:forEach items="${judgingGroups}" var="group">
                            <c:choose>
                                <c:when
                                    test="${team.judgingGroup == group}">
                                    <input type='radio'
                                        name='${team.teamNumber}'
                                        value='${group}'
                                        id='${team.teamNumber}_${group}'
                                        checked />
                                </c:when>
                                <c:otherwise>
                                    <input type='radio'
                                        name='${team.teamNumber}'
                                        value='${group}'
                                        id='${team.teamNumber}_${group}' />
                                </c:otherwise>
                            </c:choose>
                            <label for='${team.teamNumber}_${group}'>${group}</label>
                        </c:forEach>
                        <input type='radio' name='${team.teamNumber}'
                            value='text' />
                        <input type='text'
                            name='${team.teamNumber}_text' />
                    </td>
                </tr>
            </c:forEach>
        </table>

        <input type='submit' name='submit_data' value='Commit' />
        <input type='submit' name='submit_data' value='Cancel' />
    </form>


</body>
</html>

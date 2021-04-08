<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<title>Update team information (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Update team information (Upload Schedule)</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>There are some differences between the schedule and the
        database.</p>

    <form id='teamInfoDiferences'
        action='<c:url value="/schedule/CommitTeamInformationChanges"/>'
        method='POST'>

        <!-- name differences -->
        <c:if test="${not empty uploadScheduleData.nameDifferences}">
            <h2>Name changes</h2>

            <table>
                <tr>
                    <th>&nbsp;</th>
                    <th>Team #</th>
                    <th>Old Name</th>
                    <th>New Name</th>
                </tr>
                <c:forEach
                    items="${uploadScheduleData.nameDifferences }"
                    var="difference">

                    <tr>
                        <td>
                            <input type="checkbox"
                                name="name_${difference.number }"
                                id="name_g${difference.number }" checked />
                        </td>
                        <td>${difference.number }</td>
                        <td>${difference.oldName }</td>
                        <td>${difference.newName }</td>
                    </tr>

                </c:forEach>
            </table>
        </c:if>


        <!-- organization differences -->
        <c:if
            test="${not empty uploadScheduleData.organizationDifferences}">
            <h2>Organization changes</h2>

            <table>
                <tr>
                    <th>&nbsp;</th>
                    <th>Team #</th>
                    <th>Old Organization</th>
                    <th>New Organization</th>
                </tr>
                <c:forEach
                    items="${uploadScheduleData.organizationDifferences }"
                    var="difference">

                    <tr>
                        <td>
                            <input type="checkbox"
                                name="organization_${difference.number }"
                                id="organization_${difference.number }"
                                checked />
                        </td>
                        <td>${difference.number }</td>
                        <td>${difference.oldOrganization }</td>
                        <td>${difference.newOrganization }</td>
                    </tr>

                </c:forEach>
            </table>
        </c:if>

        <input type='submit' id='submit_data' value='Submit Changes' />
    </form>
    <a href='<c:url value="/schedule/CommitSchedule"/>'>
        <button>Skip all changes</button>
    </a>

</body>
</html>

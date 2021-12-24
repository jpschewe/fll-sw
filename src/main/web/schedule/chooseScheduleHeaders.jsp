<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.schedule.ChooseScheduleHeaders.populateContext(application, pageContext);
%>

<html>
<head>
<title>Choose Headers (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript"
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type="text/javascript"
    src="<c:url value='/extlib/jquery-validation/dist/jquery.validate.min.js'/>"></script>

<script type="text/javascript">
  $(document).ready(function() {
    $("#choose_headers").validate();
  });
</script>

</head>

<body>
    <h1>Choose Headers (Upload Schedule)</h1>

    <p>
        If the number of performance rounds or practice rounds does not
        match what is expected, <a
            href="<c:url value='/admin/edit_tournament_parameters.jsp'/>">edit
            the tournament parameters</a> and then upload the schedule
        again.
    </p>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="choose_headers" id="choose_headers" method='POST'
        action='ProcessHeaders'>

        <p>Highlighted columns are required, all others are
            optional.</p>

        <table border='1'>

            <tr>
                <th>Needed information</th>
                <th>Data file column name</th>
            </tr>

            <tr bgcolor='yellow'>
                <td>Team Number</td>
                <td>
                    <select name='teamNumber'>
                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <option value="${fileHeader}">${fileHeader}</option>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Team Name</td>
                <td>
                    <select name='teamName'>
                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <option value="${fileHeader}">${fileHeader}</option>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Organization</td>
                <td>
                    <select name='organization'>
                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <option value="${fileHeader}">${fileHeader}</option>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Award Group</td>
                <td>
                    <select name='awardGroup'>
                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <option value="${fileHeader}">${fileHeader}</option>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Judging Group</td>
                <td>
                    <select name='judgingGroup'>
                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <option value="${fileHeader}">${fileHeader}</option>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <c:forEach begin="1" end="${numPracticeRounds}"
                varStatus="practiceLoopStatus">
                <tr>
                    <td bgcolor='yellow'>Practice
                        ${practiceLoopStatus.index}</td>
                    <td>
                        <select
                            name='practice${practiceLoopStatus.index}'>
                            <c:forEach items="${spreadsheetHeaderNames}"
                                var="fileHeader">
                                <option value="${fileHeader}">${fileHeader}</option>
                            </c:forEach>
                        </select>
                    </td>
                </tr>
                <td bgcolor='yellow'>Practice
                    ${practiceLoopStatus.index} table</td>
                <td>
                    <select
                        name='practicefTable${practiceLoopStatus.index}'>
                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <option value="${fileHeader}">${fileHeader}</option>
                        </c:forEach>
                    </select>
                </td>
                </tr>

            </c:forEach>

            <c:forEach begin="1" end="${numSeedingRounds}"
                varStatus="perfLoopStatus">
                <tr>
                    <td bgcolor='yellow'>Performance
                        ${perfLoopStatus.index}</td>
                    <td>
                        <select name='perf${prefLoopStatus.index}'>
                            <c:forEach items="${spreadsheetHeaderNames}"
                                var="fileHeader">
                                <option value="${fileHeader}">${fileHeader}</option>
                            </c:forEach>
                        </select>
                    </td>
                </tr>
                <td bgcolor='yellow'>Performance
                    ${perfLoopStatus.index} table</td>
                <td>
                    <select name='perfTable${perfLoopStatus.index}'>
                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <option value="${fileHeader}">${fileHeader}</option>
                        </c:forEach>
                    </select>
                </td>
                </tr>

            </c:forEach>

        </table>


        <p>Match the column names from the schedule data file with
            the subjective categories that they contain the schedule
            for. Also specify the number of minutes between judging
            sessions for each category.</p>

        <table border='1'>
            <tr>
                <th>Subjective Category</th>
                <th>Data file column name</th>
                <th>Duration (minutes)</th>
            </tr>
            <c:forEach
                items="${challengeDescription.subjectiveCategories }"
                var="subcat">
                <tr>

                    <td>${subcat.title}</td>

                    <td>
                        <select name='${subcat.name}:header'>

                            <c:forEach
                                items="${uploadScheduleData.headerNames}"
                                var="subjHeader" varStatus="loopStatus">
                                <option value='${loopStatus.index}'>${subjHeader}</option>
                            </c:forEach>
                            <!-- foreach data file header -->

                        </select>
                    </td>

                    <td>
                        <input type="text"
                            name="${subcat.name}:duration"
                            id="${subcat.name}:duration"
                            value="${default_duration}"
                            class="required digits" />
                    </td>

                </tr>
                <!-- row for category -->

            </c:forEach>
            <!--  foreach category -->

        </table>

        <input type="submit" id='submit_data'
            onsubmit="return validateForm()" value="Submit" />
    </form>

</body>
</html>
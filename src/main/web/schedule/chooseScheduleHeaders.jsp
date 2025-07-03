<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.schedule.ChooseScheduleHeaders.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Choose Headers (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="chooseScheduleHeaders.js"></script>

<script type='text/javascript'>
  "use strict";

  const numPerformanceRuns = parseInt("${uploadScheduleData.numPerformanceRuns}");
</script>

</head>

<body>
    <h1>Choose Headers (Upload Schedule)</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <form name="choose_headers" id="choose_headers" method='POST'
        action="<c:url value='/schedule/ChooseScheduleHeaders'/>">

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
                    <select id='teamNumber' name='teamNumber'>
                        <c:forEach items="${numberColumns}"
                            var="fileHeader">
                            <c:choose>
                                <c:when
                                    test="${fileHeader == teamNumber_value}">
                                    <option value="${fileHeader}"
                                        selected>${fileHeader}</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${fileHeader}">${fileHeader}</option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Team Name</td>
                <td>
                    <select id='teamName' name='teamName'>
                        <option value='' selected>None</option>

                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <c:choose>
                                <c:when
                                    test="${fileHeader == teamName_value }">
                                    <option value="${fileHeader}"
                                        selected>${fileHeader}</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${fileHeader}">${fileHeader}</option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Organization</td>
                <td>
                    <select id='organization' name='organization'>
                        <option value='' selected>None</option>

                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <c:choose>
                                <c:when
                                    test="${fileHeader == organization_value}">
                                    <option value="${fileHeader}"
                                        selected>${fileHeader}</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${fileHeader}">${fileHeader}</option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Award Group</td>
                <td>
                    <select id='awardGroup' name='awardGroup'>
                        <option value='' selected>None</option>

                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <c:choose>
                                <c:when
                                    test="${fileHeader == awardGroup_value }">
                                    <option value="${fileHeader}"
                                        selected>${fileHeader}</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${fileHeader}">${fileHeader}</option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Judging Group</td>
                <td>
                    <select id='judgingGroup' name='judgingGroup'>
                        <option value='' selected>None</option>

                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <c:choose>
                                <c:when
                                    test="${fileHeader == judgingGroup_value }">
                                    <option value="${fileHeader}"
                                        selected>${fileHeader}</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${fileHeader}">${fileHeader}</option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </td>
            </tr>

            <tr>
                <td>Wave</td>
                <td>
                    <select id='wave' name='wave'>
                        <option value='' selected>None</option>

                        <c:forEach items="${spreadsheetHeaderNames}"
                            var="fileHeader">
                            <c:choose>
                                <c:when
                                    test="${fileHeader == wave_value}">
                                    <option value="${fileHeader}"
                                        selected>${fileHeader}</option>
                                </c:when>
                                <c:otherwise>
                                    <option value="${fileHeader}">${fileHeader}</option>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </select>
                </td>
            </tr>
        </table>

        <p>Performance rounds</p>
        <table border='1'>
            <tr>
                <th>Round</th>
                <th>Time</th>
                <th>Table</th>
                <th>Display Name</th>
                <th>Is Regular Match Play</th>
                <th>Display on Scoreboard</th>
            </tr>


            <c:forEach begin="1"
                end="${uploadScheduleData.numPerformanceRuns}"
                varStatus="perfLoopStatus">

                <tr>
                    <td bgcolor='yellow'>${perfLoopStatus.index}</td>
                    <td>
                        <select id='perf${perfLoopStatus.index}_time'
                            name='perf${perfLoopStatus.index}_time'>
                            <c:forEach items="${timeColumns}"
                                var="fileHeader">
                                <c:set var="value"
                                    value="${performanceRound_values[perfLoopStatus.index]}" />

                                <c:choose>
                                    <c:when
                                        test="${fileHeader == value}">
                                        <option value="${fileHeader}"
                                            selected>${fileHeader}</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value="${fileHeader}">${fileHeader}</option>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </select>
                    </td>

                    <td>
                        <select id='perf${perfLoopStatus.index}_table'
                            name='perf${perfLoopStatus.index}_table'>
                            <c:forEach items="${spreadsheetHeaderNames}"
                                var="fileHeader">
                                <option value="${fileHeader}">${fileHeader}</option>
                            </c:forEach>
                        </select>
                    </td>

                    <td>
                        <input type='text'
                            id='perf${perfLoopStatus.index}_name'
                            name='perf${perfLoopStatus.index}_name' />
                    </td>

                    <td>
                        <select id='perf${perfLoopStatus.index}_runType'
                            name='perf${perfLoopStatus.index}_runType'>
                            <c:forEach items='${runTypes}' var='runType'>
                                <c:choose>
                                    <c:when
                                        test='${performanceRound_runType[perfLoopStatus.index] == runType}'>
                                        <option value='${runType}'
                                            selected>${runType}</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value='${runType}'>${runType}</option>
                                    </c:otherwise>

                                </c:choose>
                            </c:forEach>
                        </select>
                    </td>

                    <td>
                        <c:choose>
                            <c:when
                                test="${performanceRound_scoreboard[perfLoopStatus.index] == true}">
                                <input type='checkbox'
                                    id='perf${perfLoopStatus.index}_scoreboard'
                                    name='perf${perfLoopStatus.index}_scoreboard'
                                    checked />
                            </c:when>
                            <c:otherwise>
                                <input type='checkbox'
                                    id='perf${perfLoopStatus.index}_scoreboard'
                                    name='perf${perfLoopStatus.index}_scoreboard' />
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>

            </c:forEach>

        </table>


        <p>Match the column names from the schedule data file with
            the subjective categories that they contain the schedule
            for.</p>

        <table border='1'>
            <tr>
                <th>Subjective Category</th>
                <th>Data file column name</th>
                <th>2nd Data file column name (optional)</th>
            </tr>
            <c:forEach
                items="${challengeDescription.subjectiveCategories}"
                var="subcat">
                <tr>

                    <td bgcolor='yellow'>${subcat.title}</td>

                    <td>
                        <select name='${subcat.name}:header'>

                            <c:forEach items="${timeColumns}"
                                var="subjHeader">
                                <option value='${subjHeader}'>${subjHeader}</option>
                            </c:forEach>
                            <!-- foreach data file header -->

                        </select>
                    </td>

                    <td>
                        <select name='${subcat.name}:header2'>
                            <option value="none">None</option>

                            <c:forEach items="${timeColumns}"
                                var="subjHeader">
                                <option value='${subjHeader}'>${subjHeader}</option>
                            </c:forEach>
                            <!-- foreach data file header -->

                        </select>
                    </td>

                </tr>
                <!-- row for category -->

            </c:forEach>
            <!--  foreach category -->

        </table>

        <input type="submit" id='submit_data' value="Submit" />
    </form>

</body>
</html>

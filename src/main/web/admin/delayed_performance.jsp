<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
fll.web.admin.StoreDelayedPerformance.populateContext(application, pageContext);
%>

<html>
<head>
<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/extlib/timepicker/jquery.timepicker.js'/>"></script>

<script type="text/javascript"
    src="<c:url value='/extlib/jquery-validation/dist/jquery.validate.min.js'/>"></script>

<script type="text/javascript" src="delayed_performance.js"></script>

<link rel="stylesheet"
    href="<c:url value='/extlib/timepicker/jquery.timepicker.css'/>" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>">

<link rel="stylesheet" type="text/css"
    href="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.css' />" />

<script type="text/javascript"
    src="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.js' />"></script>

<title>Delayed Performance Display</title>
</head>

<p>Scores for the specified performance run number will not be
    displayed until after the specified date and time.</p>

<p>These times only apply to the display of regular match play
    scores on the main scoreboard. They do not effect the head to head
    bracket display.</p>

<form action="<c:url value='/admin/StoreDelayedPerformance' />"
    method="POST" name="delayed_performance" id="delayed_performance">

    <table border="1" id="delayedPerformanceTable">
        <tr>
            <th>Run Number</th>
            <th>Date</th>
            <th>Time</th>
        </tr>

        <c:forEach items="${delays}" var="delay" varStatus="loopStatus">
            <tr>
                <td>
                    <input type="text"
                        name="runNumber${loopStatus.index}"
                        id="runNumber${loopStatus.index}"
                        value="${delay.runNumber}"
                        class="required digits" size="8" />
                </td>
                <td>
                    <input type="text" name="date${loopStatus.index}"
                        id="date${loopStatus.index}"
                        value="${delay.delayUntilDateString}" size="8" />
                </td>

                <td>
                    <input name="time${loopStatus.index}"
                        id="time${loopStatus.index}" type="text"
                        class="time"
                        value="${delay.delayUntilTimeString }" size="8" />
                </td>

            </tr>
        </c:forEach>
    </table>

    <input type="hidden" name="numRows" id="numRows"
        value="${fn:length(delays)}" />
    <!--  -->
    <button id='addRow'>Add Row</button>
    <!--  -->
    <input type='submit' name='commit' value='Finished'
        onclick='return validateData()' />
</form>

<body>
</body>

</html>

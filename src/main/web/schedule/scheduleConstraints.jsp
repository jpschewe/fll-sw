<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.schedule.ProcessScheduleConstraints.populateContext(application, pageContext);
%>

<html>
<head>
<title>Specify schedule constraints (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Specify schedule constraints (Upload Schedule)</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <form name="constraints" id="constraints" method='POST'
        action='ProcessScheduleConstraints'>
        <p>Specify the constraints to use for checking the uploaded
            schedule. If you haven't been given any specific values to
            use, just use the defaults.</p>

        <div>
            Time between subjective and performance events:
            <input type="number" name="changeTimeDuration"
                id="changeTimeDuration"
                value="${uploadScheduleData.schedParams.changetimeMinutes }"
                min="0" required />
            minutes
        </div>

        <div>
            Time between subjective events:
            <input name="subjectiveChangeTimeDuration"
                id="subjectiveChangeTimeDuration"
                value="${uploadScheduleData.schedParams.subjectiveChangetimeMinutes }"
                type="number" min="0" required />
            minutes
        </div>

        <div>
            Time between performance events:
            <input name="performanceChangeTimeDuration"
                id="performanceChangeTimeDuration"
                value="${uploadScheduleData.schedParams.performanceChangetimeMinutes }"
                type="number" min="0" required />
            minutes
        </div>

        <div>
            Performance duration:
            <input name="performanceDuration" id="performanceDuration"
                value="${uploadScheduleData.schedParams.performanceMinutes }"
                type="number" min="0" required />
            minutes
        </div>

        <div>
            Number of non-practice performance runs:
            <input name="numPerformanceRuns" id="numPerformanceRuns"
                type="number" min="${numSeedingRounds}"
                value="${numSeedingRounds}" required />
            <br />
        </div>


        <input type="submit" id='submit_data' value='Submit' />
    </form>

</body>
</html>

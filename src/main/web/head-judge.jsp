<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Head Judge links</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>
    <h1>Head Judge links</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>This page contains links to the pages used in the judges
        room. Most links open new tabs so that you can continue to
        follow the workflow on this page.</p>

    <p>
        The current tournament is
        <b>${tournament.description} on ${tournament.dateString}
            [${tournament.name}]</b>
    </p>

    <h2>Server addresses</h2>
    <p>These are the addresses that can be used on the judges
        electronic devices to connect to this server.</p>
    <ul>
        <c:forEach items="${urls}" var="url">
            <li class="no-marker">
                <a class="wide" href="${url }">${url }</a>
            </li>
        </c:forEach>
    </ul>


    <h2>Tournament steps</h2>
    <a class="wide" target="_subjective"
        href="<c:url value='/subjective/Auth' />">Enter subjective
        scores. This is done through the subjective web application</a>

    <a class="wide"
        href="<c:url value='/report/edit-award-winners.jsp' />"
        target="_blank">Enter the winners of awards for use in the
        awards report</a>

    <a class="wide"
        href="<c:url value='/report/edit-advancing-teams.jsp' />"
        target="_blank">Enter the advancing teams for use in the
        awards report</a>

    <a class="wide" href="<c:url value='/report/AwardsReport' />"
        target="_blank">Report of winners for the tournament. This
        can be published on the web or used for the awards ceremony.</a>

    <a class="wide" target="_report"
        href="<c:url value='/report/index.jsp' />">Generate reports
        - this is done once all of the subjective scores are in.</a>

    <h2>Finalist scheduling</h2>
    <p>This is used at tournaments where there is more than 1
        judging group in an award group. This is typically the case at a
        state tournament where all teams are competing for first place
        in each category, but there are too many teams for one judge to
        see.</p>

    <p>Before using these links the initial head to head brackets
        need to be assigned in the performance area and the performance
        dump needs to be imported into this server.</p>

    <a class="wide"
        href="<c:url value='/report/non-numeric-nominees.jsp' />"
        target="_blank">Enter non-numeric nominees. This is used to
        enter the teams that are up for consideration for the non-scored
        subjective categories. This information transfers over to the
        finalist scheduling web application. This is also used in the
        awards scripts report.</a>

    <a class="wide" href="<c:url value='/report/finalist/load.jsp' />"
        target="_blank">Schedule Finalists. Before visiting this
        page, all subjective scores need to be uploaded and any head to
        head brackets that will occur during the finalist judging should
        be created to avoid scheduling conflicts.</a>

    <div class="wide">
        <form
            ACTION="<c:url value='/report/finalist/PdfFinalistSchedule' />"
            METHOD='POST' target="_blank">
            <select name='division'>
                <c:forEach var="division" items="${finalistDivisions }">
                    <option value='${division }'>${division }</option>
                </c:forEach>
            </select>
            <input type='submit' value='Finalist Schedule (PDF)' />
        </form>
    </div>

    <a class="wide"
        href="<c:url value='/report/finalist/TeamFinalistSchedule' />"
        target="_blank">Finalist Schedule for each team</a>


</body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="JUDGE" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Judge Links</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>

</head>

<body>
    <h1>Judge links</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

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
            <li>
                <a href="${url }">${url }</a>
            </li>
        </c:forEach>
    </ul>

    <ul>
        <li>
            Enter subjective scores. This is done through the <a
                target="_subjective"
                href="<c:url value='/subjective/Auth' />">subjective
                web application</a>.
        </li>

        <li>
            <a href="<c:url value='/report/edit-award-winners.jsp' />"
                target="_blank">Enter the winners of awards for use
                in the awards report</a>
        </li>
    </ul>

    <h2>Finalist scheduling</h2>
    <p>This is used at tournaments where there is more than 1
        judging group in an award group. This is typically the case at a
        state tournament where all teams are competing for first place
        in each category, but there are too many teams for one judge to
        see.</p>

    <p>Before using these links the initial head to head brackets
        need to be assigned in the performance area and the performance
        dump needs to be imported using the link above.</p>

    <ul>

        <li>
            <a href="<c:url value='/report/non-numeric-nominees.jsp' />"
                target="_blank">Enter non-numeric nominees</a>. This is
            used to enter the teams that are up for consideration for
            the non-scored subjective categories. This information
            transfers over to the finalist scheduling web application.
            This is also used in the awards scripts report.
        </li>

    </ul>
</body>
</html>

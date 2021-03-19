<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
%>


<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>${challengeDescription.title }</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />


    <p>
        The current tournament is
        <b>${tournament.description} on ${tournament.dateString}
            [${tournament.name}]</b>
    </p>

    <h2>Computer types</h2>

    <p>There is a link here for the common uses of computers at a
        tournament.</p>

    <ul>
        <c:if test="${authentication.ref}">
            <li>
                <a href="scoreEntry/select_team.jsp">Score Entry</a> -
                follow this link on the performance score entry
                computers.
            </li>
        </c:if>

        <c:if test="${authentication.judge}">
            <li>
                <a href="subjective/Auth">Subjective Judging</a> -
                follow this link on the subjective judge's electronic
                devices.
            </li>
        </c:if>

        <li>
            <a href='display.jsp'>Big Screen Display</a> - follow this
            link on the computer that's used to display scores with the
            projector.
        </li>

        <c:if test="${authentication.admin}">
            <li>
                <a href="<c:url value='/judges-room.jsp' />">Judges
                    room</a> - follow this link on the judges room server
            </li>

            <li>
                <a href="<c:url value='/admin/performance-area.jsp' />">Scoring
                    Coordinator</a>
            </li>
        </c:if>

        <li>
            <a href="public">Public landing page</a> A list of pages
            that the public may want to visit if they are allowed to
            connect to the network.
        </li>

    </ul>

    <c:if test="${authentication.admin}">
        <h2>Pages for the head computer person</h2>
        <ul>
            <li>
                <a href="documentation/index.html">Documentation</a>
            </li>

            <li>
                <a href="troubleshooting/index.jsp">Troubleshooting</a>
            </li>

            <li>
                <a href="<c:url value='/setup'/>">Database setup</a> -
                Use this link to upload a saved database
            </li>

            <li>
                <a href="admin/index.jsp">Administration</a>
            </li>

            <li>
                <a href="playoff/index.jsp">Head to head</a>
            </li>

            <li>
                <a href="report/index.jsp">Tournament reporting</a>
            </li>

            <li>
                <a href='challenge.xml'>Challenge Descriptor</a>. One
                can use this to see the scoring and the tie breaker.
            </li>

            <li>
                <a href='BlankScoresheet'>Blank performance score
                    sheet, one regular match play and one practice (PDF)</a>
            </li>

            <c:forEach
                items="${challengeDescription.subjectiveCategories}"
                var="category">
                <li>
                    <a href="BlankSubjectiveSheet/${category.name}">Blank
                        subjective sheet - ${category.title} (PDF)</a>
            </c:forEach>

        </ul>
    </c:if>

    <c:if test="${authentication.admin}">
        <h2>Server addresses</h2>
        <p>These are needed for any computer connecting to the
            software.</p>
        <ul>
            <c:forEach items="${urls}" var="url">
                <li>
                    <a href="${url }">${url }</a>
                </li>
            </c:forEach>
        </ul>
    </c:if>

    <c:if test="${authentication.admin}">
        <h2>State computer lead</h2>
        <p>The following links are most useful for the lead for the
            state/region/partner</p>

        <ul>

            <li>
                <form id='import'
                    action="<c:url value='/developer/importdb/ImportDBDump'/>"
                    method='post' enctype='multipart/form-data'>

                    <p>Merge another database into the current
                        database.</p>
                    <input type='file' size='32' name='dbdump' />
                    <input type='submit' name='importdb'
                        value='Import Database' />
                </form>
            </li>

        </ul>
    </c:if>

    <h2>Other useful pages</h2>
    <ul>
        <li>
            <a href="DoLogout">Log out</a> Log a computer out so that
            they need to enter the password again to change scores.
        </li>

        <li>
            <a href='scoreboard/index.jsp'>Score board</a>
        </li>

        <c:if test="${authentication.admin}">
            <li>
                <a href="developer/index.jsp">Developer page</a>
            </li>
        </c:if>

        <li>
            <a href="credits/credits.jsp">Credits</a>
        </li>
    </ul>

</body>
</html>

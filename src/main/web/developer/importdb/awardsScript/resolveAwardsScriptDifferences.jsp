<%@page
    import="fll.web.developer.importdb.awardsScript.ResolveAwardsScriptDifferences"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
ResolveAwardsScriptDifferences.populateContext(pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Resolve Awards Script information differences</title>
</head>

<body>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <form name="resolveAwardsScriptDifferences"
        action="CommitAwardsScriptChanges">
        <p>There are differences in the awards script between the
            source (imported) database and the destination database. You
            need to choose to accept the value from the source database
            or the value from the destination database.</p>

        <c:forEach
            items="${importDbSessionInfo.awardsScriptDifferences}"
            var="difference" varStatus="loopStatus">
            <p>${difference.description}</p>
            <c:forEach items="${awardsScriptDifferenceActionValues}"
                var="differenceAction">
                <input type='radio'
                    name='difference_${loopStatus.index}'
                    id='difference_${loopStatus.index}_${differenceAction.name}'
                    value='${differenceAction.name}' required />
                <label
                    for='difference_${loopStatus.index}_${differenceAction.name}'>
                    ${differenceAction.description}</label>
                <br />
            </c:forEach>
            <hr />
        </c:forEach>


        <input name='submit_data' type='submit' value="Apply Changes" />
    </form>

    <p>
        Return to the <a
            href="<c:url value='/developer/importdb/selectTournament.jsp'/>">tournament
            selection page</a>.
    </p>


</body>
</html>

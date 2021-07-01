<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF" allowSetup="false" />

<%
fll.web.scoreEntry.ChooseTable.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Score Entry [Choose Table]</title>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>

</head>

<body>
    <div>Choose the table that you will be entering scores for.
        This will control the order that the teams show up in the team
        selection box.</div>

    <form action="<c:url value='/scoreEntry/ChooseTable'/>"
        method="POST">
        <select name='table'>
            <option value="__all__">All Tables</option>
            <c:forEach items="${tables}" var="table">
                <c:choose>
                    <c:when test="${scoreEntrySelectedTable == table }">
                        <option value="${table}" selected>${table}</option>
                    </c:when>
                    <c:otherwise>
                        <option value="${table}">${table}</option>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </select>

        <input type="submit" id="submit_table_choice"
            value="Save Table Being Scored">

    </form>

</body>
</html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN,JUDGE" allowSetup="false" />

<%
fll.web.report.EditAwardWinners.populateContext(application, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Edit award winners</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<link rel="stylesheet" type="text/css" href="edit-award-winners.css" />

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

</head>

<body>

    <h1>Edit Award Winners</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <div id="container">
        <c:forEach items="${categories}" var="category">
            <h1>${category.title}</h1>

            <c:set var="perAwardGroup" value="${category.perAwardGroup}" />

            <c:choose>
                <c:when test="${category.perAwardGroup}">
                    <!-- per award group award -->
                    <c:forEach items="${awardGroups}" var="awardGroup">

                        <h2>${awardGroup}</h2>
                        <c:set var="winners"
                            value="${awardGroupAwardWinners[category.title][awardGroup]}" />
                        <c:set var="categoryTitle"
                            value="${category.title}" />
                        <c:set var="awardType"
                            value="${awardTypes[category.title]}" />
                        <c:set var="ranked" value="${category.ranked}" />

                        <%@ include file="edit-award-winners-table.jspf"%>

                    </c:forEach>
                    <%-- foreach award group --%>
                    <!-- end per award group award -->
                </c:when>
                <c:otherwise>
                    <c:set var="awardGroup" value="" />
                    <c:set var="winners"
                        value="${overallAwardWinners[category.title]}" />
                    <c:set var="categoryTitle" value="${category.title}" />
                    <c:set var="awardType"
                        value="${awardTypes[category.title]}" />
                    <c:set var="ranked" value="${category.ranked}" />

                    <%@ include file="edit-award-winners-table.jspf"%>

                    <!-- end overall award -->
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</body>
</html>

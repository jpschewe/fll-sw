<%@page import="fll.web.report.ComputeJudgeSummary"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE, REPORT_GENERATOR"
    allowSetup="false" />

<%
ComputeJudgeSummary.populateContext(application, pageContext);
%>

<html>
<head>

<meta http-equiv="refresh" content="60" />

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Judge Summary</title>

<style>
.no-judge {
    background-color: red;
}

.missing-score {
    background-color: yellow;
}

.good {
    background-color: lightgreen;
}

table {
    border-collapse: collapse;
}

div.content table, div.content th, div.content td {
    border: 1px solid black;
}
</style>
</head>

<body>
    <div class='content'>
        <h1>Judge Summary</h1>

        <p>
            Below is a list of judges found and what categories they
            scored.
            <b>Note:</b>
            If there is no schedule loaded and there are multiple
            judging groups for an award group, then the number of teams
            expected will be too high.
        </p>

        <p>This page will refresh on it's own to show the most
            recent judge information.</p>

        <p>This page was generated at ${timestamp}.</p>

        <c:forEach items="${judgeSummary}" var="entry">
            <h2>${entry.key}</h2>

            <table>
                <tr>
                    <th>Category</th>
                    <th>Judge</th>
                    <th>Num Teams Expected</th>
                    <th>Num Teams Scored</th>
                    <th>Final Scores</th>
                </tr>

                <c:forEach items="${entry.value}" var="judgeInfo">
                    <tr>
                        <td>${judgeInfo.category }</td>
                        <c:choose>
                            <c:when test="${empty judgeInfo.judge}">
                                <td>&nbsp;</td>
                            </c:when>
                            <c:otherwise>
                                <td>${judgeInfo.judge}</td>
                            </c:otherwise>
                        </c:choose>
                        <td>${judgeInfo.numExpected}</td>
                        <c:choose>
                            <c:when
                                test="${judgeInfo.numExpected == judgeInfo.numActual }">
                                <td class='good'>${judgeInfo.numActual}</td>
                            </c:when>
                            <c:when test="${empty judgeInfo.judge }">
                                <td class='no-judge'>${judgeInfo.numActual}</td>
                            </c:when>
                            <c:otherwise>
                                <td class='missing-score'>${judgeInfo.numActual}</td>
                            </c:otherwise>
                        </c:choose>
                        <c:choose>
                            <c:when test="${judgeInfo.finalScores}">
                                <td class='good'>YES</td>
                            </c:when>
                            <c:otherwise>
                                <td>NO</td>
                            </c:otherwise>
                        </c:choose>
                    </tr>
                </c:forEach>
            </table>

        </c:forEach>
    </div>
</body>
</html>

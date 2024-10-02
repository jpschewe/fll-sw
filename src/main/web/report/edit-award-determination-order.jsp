<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.web.report.EditAwardDeterminationOrder.populateContext(application, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Edit Awards Determination Order</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript"
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript" src="edit-award-determination-order.js"></script>

</head>

<body>

    <form method='POST' action='EditAwardDeterminationOrder'>
        <input type="hidden" name="referer" value="${header. referer}" />

        <div id='award_order'>
            <c:forEach items="${awards}" var="award">
                <div class='award_container'>
                    <button type='button' class='move_up'>Move
                        Up</button>
                    <button type='button' class='move_down'>Move
                        Down</button>
                    <input type='hidden' name='${award.title}' />
                    ${award.title}
                </div>
            </c:forEach>
        </div>

        <input type="submit" id="submit_data" value="Submit" />
    </form>

</body>
</html>

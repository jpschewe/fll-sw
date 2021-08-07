<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.EditLevels.populateContext(application, pageContext);
%>

<html>
<head>

<title>Edit Tournament Levels</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript" src="edit_levels.js"></script>

<script type="text/javascript">
  function init() {
    <c:forEach items="${levels}" var="level">
    addRow("${level.id}", "${level.name}");
    </c:forEach>

    <c:forEach items="${levels}" var="level">
    <c:choose>
    <c:when test="${level.nextLevelId == NO_NEXT_LEVEL_ID}">
    selectNextLevel("${level.id}", "none");
    </c:when>
    <c:otherwise>
    selectNextLevel("${level.id}", "${level.nextLevelId}");
    </c:otherwise>
    </c:choose>
    </c:forEach>
  }
</script>

</head>
<body>

    <form id="level_form">

        <table id="level_table">
            <tr>
                <th>Name</th>
                <th>Next Level</th>
                <th></th>
            </tr>

        </table>

        <button type="button" id="add">Add Level</button>
        <button type="button" id="save">Save Changes</button>

    </form>

</body>

</html>
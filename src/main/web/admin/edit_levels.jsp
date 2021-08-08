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
  // keep constants consistent between Java and Javascript
  const NEW_LEVEL_ID_PREFIX = "${NEW_LEVEL_ID_PREFIX}";
  const NEXT_PREFIX = "${NEXT_PREFIX}";
  const NONE_OPTION_VALUE = "${NONE_OPTION_VALUE}";
  const NONE_OPTION_TITLE = "${NONE_OPTION_TITLE}";

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

    <form id="level_form" action="EditLevels" method="POST">

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
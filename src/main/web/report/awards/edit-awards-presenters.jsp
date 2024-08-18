<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REPORT_GENERATOR" allowSetup="false" />

<%
fll.web.report.awards.EditAwardsPresenters.populateContext(application, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Edit Awards Presenters</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<link rel="stylesheet" type="text/css" href="edit-awards-script.css" />

<script type="text/javascript"
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript" src="edit-awards-script.js"></script>

<script type="text/javascript" src="edit-awards-presenters.js"></script>

<script type="text/javascript">
function pageInit() {
  awardsScriptModule.configureTextEntry([], "${CATEGORY_CHAMPIONSHIP_PRESENTER.identifier}", ${sectionSpecified[CATEGORY_CHAMPIONSHIP_PRESENTER]});
  awardsScriptModule.configureTextEntry([], "${CATEGORY_HEAD2HEAD_PRESENTER.identifier}", ${sectionSpecified[CATEGORY_HEAD2HEAD_PRESENTER]});
  awardsScriptModule.configureTextEntry([], "${CATEGORY_PERFORMANCE_PRESENTER.identifier}", ${sectionSpecified[CATEGORY_PERFORMANCE_PRESENTER]});
  
  <c:forEach items="${subjectiveCategories}" var="category">
  awardsScriptModule.configurePresenterEntry("category_${category.name}", ${subjectiveCategoryPresenterSpecified[category]});
  </c:forEach>
  
  <c:forEach items="${nonNumericCategories}" var="category">
  awardsScriptModule.configurePresenterEntry("category_${category.title}", ${nonNumericCategoryPresenterSpecified[category]});  
  </c:forEach>
}
</script>
<body>

    <form action="EditAwardsPresenters" method="POST">

        <input type="hidden" name="level" value="${tournamentLevel.id}" />
        <input type="hidden" name="tournament"
            value="${tournament.tournamentID}" />

        <p>Editing the awards presenters for the ${descriptionText}.
            Check the box to change the value for this tournament.</p>

        <h3>${championshipAwardTitle}</h3>
        <div>
            <input type="checkbox"
                name="${CATEGORY_CHAMPIONSHIP_PRESENTER.identifier}_specified"
                id="${CATEGORY_CHAMPIONSHIP_PRESENTER.identifier}_specified" />
            <input type="text"
                name="${CATEGORY_CHAMPIONSHIP_PRESENTER.identifier}_text"
                id="${CATEGORY_CHAMPIONSHIP_PRESENTER.identifier}_text"
                value="${sectionText[CATEGORY_CHAMPIONSHIP_PRESENTER]}" />
        </div>

        <h3>${head2headAwardTitle}</h3>
        <div>
            <input type="checkbox"
                name="${CATEGORY_HEAD2HEAD_PRESENTER.identifier}_specified"
                id="${CATEGORY_HEAD2HEAD_PRESENTER.identifier}_specified" />
            <input type="text"
                name="${CATEGORY_HEAD2HEAD_PRESENTER.identifier}_text"
                id="${CATEGORY_HEAD2HEAD_PRESENTER.identifier}_text"
                value="${sectionText[CATEGORY_HEAD2HEAD_PRESENTER]}" />
        </div>

        <h3>${performanceAwardTitle}</h3>
        <div>
            <input type="checkbox"
                name="${CATEGORY_PERFORMANCE_PRESENTER.identifier}_specified"
                id="${CATEGORY_PERFORMANCE_PRESENTER.identifier}_specified" />
            <input type="text"
                name="${CATEGORY_PERFORMANCE_PRESENTER.identifier}_text"
                id="${CATEGORY_PERFORMANCE_PRESENTER.identifier}_text"
                value="${sectionText[CATEGORY_PERFORMANCE_PRESENTER]}" />
        </div>

        <h3>Subjective awards</h3>
        <c:forEach items="${subjectiveCategories}" var="category">
            <h4>Category ${category.title}</h4>
            <div>
                <input type="checkbox"
                    name="category_${category.name}_presenter_specified"
                    id="category_${category.name}_presenter_specified" />
                <input type="text"
                    name="category_${category.name}_presenter_text"
                    id="category_${category.name}_presenter_text"
                    value="${subjectiveCategoryPresenter[category]}" />
            </div>
        </c:forEach>

        <h3>Non-numeric awards</h3>
        <div>
            The categories that have awards are <a
                href="<c:url value='/report/awards/edit-categories-awarded.jsp'/>"
                target="_blank">specified per tournament level</a>.
        </div>
        <c:forEach items="${nonNumericCategories}" var="category">
            <h4>Category ${category.title}</h4>
            <div>
                <input type="checkbox"
                    name="category_${category.title}_presenter_specified"
                    id="category_${category.title}_presenter_specified" />
                <input type="text"
                    name="category_${category.title}_presenter_text"
                    id="category_${category.title}_presenter_text"
                    value="${nonNumericCategoryPresenter[category]}" />
            </div>
        </c:forEach>

        <input type="submit" id="submit_data" value="Submit" />
    </form>
    <!-- before end body -->
</body>
<!-- before html -->
</html>
<!--  end -->
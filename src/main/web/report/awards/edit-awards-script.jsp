<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.report.awards.EditAwardsScript.populateContext(request, application, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Edit Awards Script</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<link rel="stylesheet" type="text/css" href="edit-awards-script.css" />

<script type="text/javascript"
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript" src="edit-awards-script.js"></script>

<script type="text/javascript">
awardsScriptModule.init = ( )=> {
  const macros = [];
  
  // FIXME loop over macros known in Java
  macros.push(awardsScriptModule.createMacro("numTrainedOfficials", "Number of Trained Officials"));
  macros.push(awardsScriptModule.createMacro("hostSchool", "Host School"));
  macros.push(awardsScriptModule.createMacro("tournamentDirectors", "Tournament Directors"));
  macros.push(awardsScriptModule.createMacro("tournamentLevel", "Tournament Level"));
  macros.push(awardsScriptModule.createMacro("tournamentNextLevel", "Next Tournament Level"));
  macros.push(awardsScriptModule.createMacro("numTeamsAdvancing", "Number of Teams Advancing"));

  <c:forEach items="${sections}" var="section"> 
  awardsScriptModule.configureTextEntry(macros, "${section}", ${sectionSpecified[section]});
  </c:forEach>
  
  <c:forEach items="${subjectiveCategories}" var="category">
  awardsScriptModule.configureTextEntry(macros, "category_${category.name}", ${subjectiveCategorySpecified[category]});
  </c:forEach>
  
  <c:forEach items="${nonNumericCategories}" var="category">
  awardsScriptModule.configureTextEntry(macros, "category_${category.title}", ${nonNumericCategorySpecified[category]});
  </c:forEach>

};

</script>
<body>

    <form action="EditAwardsScript" method="POST">
        <div>Editing the awards script for the ${descriptionText}.</div>

        <div>The text specified for a tournament level overrides
            the text specified for the season. The text specified for a
            tournament overrides the text specified at the tournament's
            level.</div>
        <c:choose>
            <c:when test="${not empty level}">
                <div>The checkbox next to each section specifies
                    if the value should be entered here or inherited
                    from the season script. If the box is checked, what
                    is entered here will be used. If the box is
                    unchecked the value from the season script is used.
                    This is particularly important to note if the text
                    in the season script is edited after the text for
                    the tournament script. In this case if the checkbox
                    is checked, the changes from the season script will
                    not be seen.</div>
            </c:when>
            <c:when test="${not empty tournament}">
                <div>The checkbox next to each section specifies
                    if the value should be entered here or inherited
                    from the tournament level script. If the box is
                    checked, what is entered here will be used. If the
                    box is unchecked the value from the tournament level
                    script is used. This is particularly important to
                    note if the text in the tournament level script is
                    edited after the text for the tournament script. In
                    this case if the checkbox is checked, the changes
                    from the tournament level will not be seen.</div>
            </c:when>
        </c:choose>

        <div>The text sections have macros available that can be
            inserted into the text. These will be replaced with the
            appropriate value when the report is generated. Put the
            cursor where you want to insert the macro and then click the
            appropriate macro button.</div>


        <div>
            <c:choose>
                <c:when test="${not empty tournament}">
${tournament.title} - ${tournament.dateString}
</c:when>
                <c:otherwise>
                    <i>Tournament Location - Date</i>
                </c:otherwise>
            </c:choose>
        </div>
        <hr />

        <c:set var="sectionName" value="front_matter" />
        <c:set var="sectionText">
        ${frontMatterText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />


        <c:set var="sectionName" value="sponsors_intro" />
        <c:set var="sectionText">
        ${sponsorsIntroText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />


        <div>FIXME: disable editing unless the checkbox is checked</div>
        <label>
            Specify value:
            <input type="checkbox" name="sponsors_names_specified" />
        </label>
        <div>FIXME: user entered sponsor names</div>
        <hr />

        <c:set var="sectionName" value="sponsors_recognition" />
        <c:set var="sectionText">
        ${sponsorsRecognitionText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />


        <c:set var="sectionName" value="volunteers" />
        <c:set var="sectionText">
        ${volunteersText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />


        <div>FIXME: specify award order, iterates through all
            awards and then allows them to be moved up and down.
            Checkbox to see if it is being specified</div>
        <label>
            Specify value:
            <input type="checkbox" name="award_order_specified" />
        </label>
        <table>
            <tr>
                <th>Award</th>
            </tr>

        </table>

        <div>
            Enter the text for each award. The information about why
            each team received an award is entered in the <a
                href="<c:url value='/report/edit-award-winners.jsp'/>"
                target="_blank">awards for the tournament</a>.
        </div>

        <h1>Awards</h1>

        <h2>Championship</h2>
        <div>
            Presenter:
            <input type="text" />
        </div>
        <c:set var="sectionName" value="category_championship" />
        <c:set var="sectionText">
        ${categoryChampionshipText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />

        <h2>Robot Performance Awards</h2>
        <div>
            Presenter:
            <input type="text" />
        </div>
        <c:set var="sectionName" value="category_performance" />
        <c:set var="sectionText">
        ${categoryPerformanceText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />

        <h2>Subjective awards</h2>
        <c:forEach items="${subjectiveCategories}" var="category">
            <h3>Category ${category.title}</h3>
            <div>
                Presenter:
                <input type="text" />
            </div>
            <c:set var="sectionName" value="category_${category.name}" />
            <c:set var="sectionText">
        ${subjectiveCategoryText[category.name]}
        </c:set>
            <%@ include file="edit-awards-script_textarea-macros.jspf"%>
            <hr />
        </c:forEach>

        <!-- FIXME: make the default text for non-numeric categories be the description of the category -->
        <h2>Non-numeric awards</h2>
        <div>
            The categories that have awards are <a
                href="<c:url value='/report/awards/edit-categories-awarded.jsp'/>"
                target="_blank">specified per tournament level</a>.
        </div>
        <c:forEach items="${nonNumericCategories}" var="category">
            <h3>Category ${category.title}</h3>
            <div>
                Presenter:
                <input type="text" />
            </div>
            <c:set var="sectionName" value="category_${category.title}" />
            <c:set var="sectionText">
        ${nonNumericCategoryText[category.title]}
        </c:set>
            <%@ include file="edit-awards-script_textarea-macros.jspf"%>
            <hr />
        </c:forEach>


        <c:set var="sectionName" value="end_awards" />
        <c:set var="sectionText">
        ${endAwardsText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />

        <div>
            <b>The advancing teams will be listed here when the
                report is generated.</b>
        </div>
        <hr />


        <c:set var="sectionName" value="footer" />
        <c:set var="sectionText">
        ${footerText}
        </c:set>
        <%@ include file="edit-awards-script_textarea-macros.jspf"%>
        <hr />

        <input type="submit" value="Submit" />

    </form>

</body>
</html>
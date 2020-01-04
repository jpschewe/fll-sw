<%@ include file="/WEB-INF/jspf/init.jspf"%>

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Edit award winners</title>

<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/extlib/jquery.json-2.5-pre.min.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/extlib/jstorage-0.4.11.min.js'/>"></script>

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="edit-award-winners.js"></script>

</head>

<body>

    <h1>Edit Award Winners</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <h2>Instructions</h2>
    <p>Enter team numbers in the first box for each award. Enter the
        description for the award in the space under the team; this
        field may be left blank if no description is needed.</p>

    <h2>Challenge awards</h2>
    <p>Specify the winners for each of the subjective categories
        defined in the challenge description.</p>
    <ul id="challenge-award-winners"></ul>

    <h2>Extra awards per award group</h2>
    <p>Specify the winners of additional awards that are per award
        group.</p>
    <button id="extra-award-winners_add-category">Add Category</button>
    <ul id="extra-award-winners"></ul>

    <h2>Extra overall awards</h2>
    <p>Specify the winners of awards that are not per award group
        (judges, etc.)</p>
    <button id="overall-award-winners_add-category">Add
        Category</button>
    <ul id="overall-award-winners"></ul>

    <h2>Advancing Teams</h2>
    <p>Specify the teams advancing to the next tournament. The group
        names will be used in the awards report.</p>
    <button id="advancing-teams_add-group">Add Group</button>
    <ul id="advancing-teams"></ul>

    <button id="store_winners">Store Data</button>
</html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Edit advancing teams</title>

<script type='text/javascript'
    src="<c:url value='/extlib/jStorage/jstorage.min.js'/>"></script>

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="<c:url value='/js/fll-teams.js'/>"></script>

<script type='text/javascript' src="edit-advancing-teams.js"></script>

</head>

<body>

    <h1>Edit Advancing Teams</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>Specify the teams advancing to the next tournament. The group
        names will be used in the awards report.</p>
    <button id="advancing-teams_add-group">Add Group</button>
    <ul id="advancing-teams"></ul>


    <h2>Order of award groups</h2>
    <p>Specify the sort order for the award groups when printing.
        Put a number next to each group name. The groups will be output
        from lowest number to highest.</p>
    <ul id="award-group-order"></ul>

    <button id="store_winners">Store Data</button>
</html>

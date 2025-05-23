<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<!DOCTYPE HTML>
<html>
<head>
<title>Edit advancing teams</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="<c:url value='/js/fll-teams.js'/>"></script>

<script type='text/javascript' src="edit-advancing-teams.js"></script>

<script type="text/javascript">
  // award group to filter to, may be empty
  const awardGroup = "${awardGroup}";
</script>

</head>

<body>

    <h1>Edit Advancing Teams</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <p>Specify the teams advancing to the next tournament. The group
        names will be used in the awards report.</p>

    <p>Note that if two people are using this form at the same time,
        the last one to save overwrites any previous data.</p>

    <button id="advancing-teams_add-group">Add Group</button>
    <ul id="advancing-teams"></ul>


    <h2>Order of award groups</h2>
    <p>Specify the sort order for the award groups when printing.
        This is used in both the awards report and the awards script.
        Put a number next to each group name. The groups will be output
        from lowest number to highest.</p>
    <ul id="award-group-order"></ul>

    <button id="store_winners">Store Data</button>
</body>
</html>

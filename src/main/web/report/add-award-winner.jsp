<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="JUDGE" allowSetup="false" />

<!DOCTYPE HTML>
<html>
<head>
<title>Add award winner</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="<c:url value='/js/fll-teams.js'/>"></script>

<script type='text/javascript' src="add-award-winner.js"></script>

<script type="text/javascript">
  const awardGroup = "${param.awardGroup}";
</script>

</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <h1>Add winner to ${param.categoryTitle}</h1>
    <form action="AddAwardWinner" method="POST" id="add_award_winner">

        <input type="hidden" name="categoryTitle"
            value="${param.categoryTitle}" />
        <input type="hidden" name="awardGroup"
            value="${param.awardGroup}" />
        <input type="hidden" name="awardType" value="${param.awardType}" />

        <div>
            <label for="place">Place:</label>
            <input type="number" min="1" required name="place"
                id="place" />
        </div>

        <div>
            <label for="teamNumber">Team Number:</label>
            <input type="number" required name="teamNumber"
                id="teamNumber" />
        </div>

        <div>
            <label for="teamName">Team Name:</label>
            <input type="text" readonly disabled id="teamName" />
        </div>

        <div>
            <label for="organization">Organization:</label>
            <input type="text" readonly disabled id="organization" />
        </div>

        <div>
            <label for="awardGroup">Award Group:</label>
            <input type="text" readonly disabled id="awardGroup" />
        </div>

        <div>
            <label for="description">Description:</label>
            <textarea name="description" rows="10" cols="50"></textarea>
        </div>

        <div>
            <input type="submit" value="Add Team" />
        </div>

        <div>
            <button id="cancel">Cancel Add of Team</button>
        </div>

    </form>

</body>
</html>
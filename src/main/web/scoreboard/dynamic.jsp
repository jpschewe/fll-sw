<!DOCTYPE html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<html>
<head>
<title>Scoreboard</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<link rel='stylesheet' type='text/css' href='score_style.css' />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript" src="dynamic.js"></script>

<script type=text/javascript " src="set-font-size.js"></script>


</head>
<body class='scoreboard'>

    <div id='most_recent'>
        <table id="most_recent_table">
        </table>
    </div>

</body>
</html>

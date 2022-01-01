<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.scoreboard.Title.populateContext(application, session, pageContext);
%>

<html>
<head>
<meta http-equiv='refresh' content='90' />

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/base.css'/>" />
<link rel='stylesheet' type='text/css' href='score_style.css' />

<script type="text/javascript" src="set-font-size.js"></script>


</head>
<body bgcolor='#008000'>
    <div class='center bold'>
        <font face='arial' size='3' color='#ffffff'>${judgeGroupTitle}<br />
            ${ScorePageText }
        </font>
    </div>
</body>
</html>

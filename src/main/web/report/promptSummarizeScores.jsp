<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="JUDGE" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Prompt Summarize Scores</title>
</head>

<body>

    <form action="PromptSummarizeScores" method="post">

        <p>The summarized scores are out of date. What do you want
            to do?</p>

        <!-- compute -->
        <input type='submit' name='recompute'
            value='Compute Summarized Scores' />
        (Recommended)
        <br />

        <!-- skip -->
        <input type='submit' name='skipSummarization'
            value='Skip Summarization' />

    </form>

</body>
</html>

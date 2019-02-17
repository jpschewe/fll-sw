<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
<title>Prompt Summarize Scores</title>
</head>

<body>

  <form
    action="PromptSummarizeScores"
    method="post">

    <p>The summarized scores are out of date. What do you want to
      do?</p>

    <input
      type='submit'
      name='recompute'
      value='Compute Summarized Scores' /> <input
      type='submit'
      name='skipSummarization'
      value='Skip Summarization' />

  </form>

</body>
</html>

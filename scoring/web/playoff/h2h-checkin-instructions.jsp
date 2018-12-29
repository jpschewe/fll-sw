<%@ include file="/WEB-INF/jspf/init.jspf"%>
<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>

<title>Head to Head check-in instructions</title>
</head>
<body>

	<h1>Head to Head check-in instructions</h1>

	<p>These instructions explain how to setup the displays so that you
		have a display setup that allows one to see the current head to head
		status on the the tablets or a separate computer.</p>

	<ol>
		<li><a target='_display_control'
			href="<c:url value='/admin/remoteControl.jsp' />">Visit the
				remote control page</a> - this opens a new tab</li>

		<li>Make sure that all of the displays have names</li>

		<li>Setup the "Default" display to show all brackets by using the
			"Add Bracket" button.</li>

		<li>Visit the "Big Screen Display" link on the tablets</li>

		<li>If presented with a page asking for the name of the tablet,
			just click submit and continue.</li>

	</ol>


</body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.DisplayNames"%>


<html>
<head>

<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />

<title>Display</title>
<script
  type="text/javascript"
  src="<c:url value='/code.icepush'/>"></script>

<script
  type="text/javascript"
  src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>

<script type='text/javascript'>
	var width = screen.width - 10;
	var height = screen.height - 10;
	var newWindow = null;
	var str = 'height='
			+ height
			+ ',width='
			+ width
			+ ',toolbar=0,menubar=0,scrollbars=0,location=0,directories=0,status=0,resizable=0,fullscreen=1,left=0,screenX=0,top=0,screenY=0';
	var connected = true;

	function displayPage(url) {
		if (null == newWindow || newWindow.location.pathname != url) {
			newWindow = window.open(url, 'displayWindow', str);
		}
	};

	function pollSuccess(newURL) {
		if (!connected) {
			connected = true;

			// display index for a second so that the browser doesn't cache
			displayPage('<c:url value="/public"/>');

			// display the page that we want to see
			setTimeout(function() {
				displayPage(newURL);
			}, 1000);
		} else {
			displayPage(newURL);
		}
	};

	function pollFailure() {
		connected = false;
	};

	function update() {
		$.getJSON("<c:url value='/ajax/DisplayQuery'/>", function(data) {
			pollSuccess(data.displayURL);
		}).fail(function(data) {
			pollFailure();
		});
	}

	function onLoad() {
		update();
		setInterval(update, 60000);
	}
	$(document).ready(onLoad);
</script>
<icep:register
  group="display"
  callback="function(){update();}" />
</head>


<body>

  <h1>Big Screen Display Control page</h1>

  <c:if test="${not empty param.name}">
    <%
      DisplayNames.appendDisplayName(application, session, request.getParameter("name"));
    %>
    <p>Display set to ${displayName}</p>
  </c:if>

  <c:if test="${not empty displayName}">
    <p>This display is named ${displayName}</p>
  </c:if>

  <p>Leave this page open on the display computer. It's used to
    control the actual display window. You may need to press F11 in the
    newly opened window to remove the titlebar and make it fullscreen.</p>

  <form
    action="display.jsp"
    method="POST">
    Name this display computer: <input
      name="name"
      type="text"
      size="40"
      value="${displayName}" /><br /> <input
      type='submit'
      value='Submit' />
  </form>


</body>
</html>

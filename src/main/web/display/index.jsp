<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.DisplayInfo"%>

<%
fll.web.display.DisplayIndex.populateContext(request, application, pageContext);
%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<html>
<head>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<title>Display</title>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>

<script type="text/javascript" src="index.js"></script>

<script type="text/javascript">
  const REGISTER_DISPLAY_MESSAGE_TYPE = "${REGISTER_DISPLAY_MESSAGE_TYPE}";
  const PING_MESSAGE_TYPE = "${PING_MESSAGE_TYPE}";
  const ASSIGN_UUID_MESSAGE_TYPE = "${ASSIGN_UUID_MESSAGE_TYPE}";
  const DISPLAY_URL_MESSAGE_TYPE = "${DISPLAY_URL_MESSAGE_TYPE}";
  const PARAM_DISPLAY_UUID = "${param.display_uuid}";
</script>
</head>

<!-- FIXME don't open a page until a URL message is received after the UUID is stored -->
<!-- FIXME use setInterval to send a ping message every minute -->

<body>

    <h1>Big Screen Display Control page</h1>

    <p>Leave this page open on the display computer. It's used to
        control the actual display window. You may need to press F11 in
        the newly opened window to remove the titlebar and make it
        fullscreen.</p>

    <div id='name_needed'>This display needs a name before it will
        show anything. Please fill in the name below.</div>

    <div id='name_set'>
        This display is named "
        <span id='display_name'></span>
        ". You can change it's name below.
    </div>

    <div>
        Name this display:
        <input id='name' name="name" type="text" size="40"
            value="${displayName}" />
        <br />
        <button id='name_button' type='button'>Save Name</button>
    </div>

</body>
</html>

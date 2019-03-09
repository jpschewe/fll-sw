<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.DisplayInfo"%>


<html>
<head>

<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />

<title>Display</title>

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
  var socket = null;
  
  function displayPage(url) {
    if (null == newWindow || newWindow.location.pathname != url) {
      newWindow = window.open(url, 'displayWindow', str);
    }
  };

  function pollSuccess(newURL) {
    if (!connected) {
      connected = true;

      // display welcome for a second so that the browser doesn't cache the page currenty displayed
      displayPage('<c:url value="/welcome.jsp"/>');

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

    console.log("Got failure getting current display page, reconnecting...");

    if (null != socket) {
      socket.onclose = function() {
      }; // ensure that the closeSocket method doesn't get called and cause a race
      socket.close();
      socket = null;
    }

    // open the socket a second later
    setTimeout(openSocket, 1000);
  };

  function update() {
    $.getJSON("<c:url value='/ajax/DisplayQuery'/>", function(data) {
      pollSuccess(data.displayURL);
    }).fail(function(data) {
      pollFailure();
    });
  }

  function onLoad() {
    openSocket();
  }
  $(document).ready(onLoad);
</script>

<script type="text/javascript">
  function messageReceived(event) {
    console.log("received: " + event.data);

    // data doesn't matter, just execute reload on any message
    update();
  }

  function socketOpened(event) {
    console.log("Socket opened");
  }

  function socketClosed(event) {
    console.log("Socket closed");

    // open the socket a second later
    setTimeout(openSocket, 1000);
  }

  function openSocket() {
    console.log("opening socket");

    var url = window.location.pathname;
    var directory = url.substring(0, url.lastIndexOf('/'));
    var webSocketAddress = "ws://" + window.location.host + directory
        + "/DisplayWebSocket";

    socket = new WebSocket(webSocketAddress);
    socket.onmessage = messageReceived;
    socket.onopen = socketOpened;
    socket.onclose = socketClosed;

    update();
  }
</script>
</head>


<body>

  <h1>Big Screen Display Control page</h1>

  <c:if test="${not empty param.name}">
    <%
      DisplayInfo.appendDisplayName(application, session, request.getParameter("name"));
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

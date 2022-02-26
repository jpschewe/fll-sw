<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%@ page import="fll.web.playoff.RemoteControlBrackets"%>

<%
RemoteControlBrackets.populateContext(application, session, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<link rel="stylesheet" type="text/css"
    href="<c:url value='/scoreboard/score_style.css'/>" />

<style type='text/css'>
TD.Leaf {
    color: #ffffff;
    background-color: #000000
}

TD.Bridge {
    background-color: #808080
}

SPAN.TeamNumber {
    color: #ff8080;
}

SPAN.TeamName {
    color: #ffffff;
}

SPAN.TeamScore {
    color: #ffffff;
    font-weight: bold;
}

SPAN.TIE {
    color: #ff0000;
}

.TABLE_ASSIGNMENT {
    font-family: monospace;
    font-size: small;
    background-color: white;
    padding-left: 5%;
    padding-right: 5%;
}
</style>

<script type="text/javascript"
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript">
  var allBracketData = ${allBracketDataJson};
  var scrollRate = parseInt("${scrollRate}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree
  var maxNameLength = parseInt("${maxNameLength}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree
</script>

<script type='text/javascript' src="<c:url value='/js/fll-functions.js' />"></script>
<script type='text/javascript' src='h2hutils.js'></script>
<script type='text/javascript' src='remoteControlBrackets.js'></script>

<script type="text/javascript">
let prevScrollTimestamp = 0;
const secondsBetweenScrolls = parseFloat("${scrollRate}");
const pixelsToScroll = 1;
let scrollingDown = true;

function doScroll(timestamp) {
  const diff = timestamp - prevScrollTimestamp;
  if (diff >= secondsBetweenScrolls) {
      if(scrollingDown && elementIsVisible(document.getElementById("bottom"))) {
          scrollingDown = false;
      } else if(!scrollingDown && elementIsVisible(document.getElementById("top"))) {
          scrollingDown = true;
      }

      const scrollAmount = scrollingDown ? pixelsToScroll : -1 * pixelsToScroll;
      window.scrollBy(0, scrollAmount);
      prevScrollTimestamp = timestamp;
  }

  requestAnimationFrame(doScroll);
}

  document.addEventListener("DOMContentLoaded", function() {
    <c:if test="${empty param.scroll}">
    requestAnimationFrame(doScroll);
    </c:if>
    colorTableLabels();
  });
</script>
</head>
<body>
    <span id="top">&nbsp;</span>

    <br />
    <c:forEach items="${allBracketData}" var="bracketData">

        <div class='center'>Head to Head Round
            ${bracketData.firstRound}, Head to Head Bracket
            ${bracketData.bracketName}</div>
        <br />
                        
   ${bracketData.bracketOutput}
     <c:if test="${allBracketData.size() > 1}">
            <br />
            <br />
            <hr />
            <br />
            <br />
        </c:if>
    </c:forEach>
    <span id="bottom">&nbsp;</span>

</body>
</html>

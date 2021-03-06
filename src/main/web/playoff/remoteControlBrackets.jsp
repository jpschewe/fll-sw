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
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type="text/javascript"
    src="<c:url value='/extlib/jquery.scrollTo/jquery.scrollTo.min.js'/>"></script>

<script type="text/javascript">
  var allBracketData = ${allBracketDataJson};
  var scrollDuration = parseInt("${scrollDuration}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree
  var maxNameLength = parseInt("${maxNameLength}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree
</script>

<script type='text/javascript' src="<c:url value='/js/fll-functions.js' />"></script>
<script type='text/javascript' src='h2hutils.js'></script>
<script type='text/javascript' src='remoteControlBrackets.js'></script>

<script type="text/javascript">
  function scrollToBottom() {
    $.scrollTo($("#bottom"), {
      duration : scrollDuration,
      easing : 'linear',
      onAfter : scrollToTop,
    });
  }

  function scrollToTop() {
    $.scrollTo($("#top"), {
      duration : scrollDuration,
      easing : 'linear',
      onAfter : scrollToBottom,
    });
  }

  $(document).ready(function() {
    <c:if test="${empty param.scroll}">
    scrollToBottom();
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

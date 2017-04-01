<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.RemoteControlBrackets"%>

<%
  RemoteControlBrackets.populateContext(application, session, pageContext);
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
<link
  rel="stylesheet"
  type="text/css"
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
<script
  type="text/javascript"
  src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script
  type="text/javascript"
  src="<c:url value='/extlib/jquery.scrollTo-2.1.2.min.js'/>"></script>
  
<script type="text/javascript">
  var allBracketInfo = ${allBracketInfoJson};  
</script>

  
<script type="text/javascript">
  var ajaxURL = '<c:url value="/ajax/"/>';
  var rows = parseInt("${numRows}"); // could be here directly as an intger, but the JSTL and auto-formatting don't agree
  var maxNameLength = parseInt("${maxNameLength}"); // could be here directly as an intger, but the JSTL and auto-formatting don't agree

  var displayStrings = new Object();
  displayStrings.parseTeamName = function(team) {
    if (team.length > maxNameLength) {
      return team.substring(0, maxNameLength - 3) + "...";
    } else {
      return team;
    }
  }
  displayStrings.getSpecialString = function(id, data) {
    return "<span class=\"TeamName\">"
        + displayStrings.parseTeamName(data.team.teamName) + "</span>";
  }
  displayStrings.getTeamNameString = function(id, data) {
    return "<span class=\"TeamNumber\">#" + data.team.teamNumber
        + "</span> <span class=\"TeamName\">"
        + displayStrings.parseTeamName(data.team.teamName) + "</span>";
  }
  displayStrings.getTeamNameAndScoreString = function(id, data, scoreData) {
    if (scoreData != "No Show") {
      scoreData += ".0";
    }
    return "<span class=\"TeamNumber\">#" + data.team.teamNumber
        + "</span> <span class=\"TeamName\">"
        + displayStrings.parseTeamName(data.team.teamName)
        + "</span><span class=\"TeamScore\"> Score: " + scoreData + "</span>";
  }

  var validColors = new Array();
  validColors[0] = "maroon";
  validColors[1] = "red";
  validColors[2] = "orange";
  validColors[3] = "yellow";
  validColors[4] = "olive";
  validColors[5] = "purple";
  validColors[6] = "fuchsia";
  validColors[7] = "white";
  validColors[8] = "lime";
  validColors[9] = "green";
  validColors[10] = "navy";
  validColors[11] = "blue";
  validColors[12] = "aqua";
  validColors[13] = "teal";
  validColors[14] = "black";
  validColors[15] = "silver";
  validColors[16] = "gray";
  //colors sourced from http://www.w3.org/TR/CSS2/syndata.html#color-units

  function placeTableLabel(lid, table, dbLine) {
    if (table != undefined) {
      $("#" + lid + "-table").text(table);
    }
  }

  function colorTableLabels() {
    $(".table_assignment").each(
        function(index, label) {
          //Sane color? Let's start by splitting the label text
          if ($.inArray(label.innerHTML.split(" ")[0].toLowerCase(),
              validColors) > 0) {
            label.style.borderColor = label.innerHTML.split(" ")[0];
            label.style.borderStyle = "solid";
          }
        });
  }

  function scrollToBottom() {
    $.scrollTo($("#bottom"), {
      duration : rows * 1000,
      easing : 'linear',
      onAfter : scrollToTop,
    });
  }

  function scrollToTop() {
    $.scrollTo($("#top"), {
      duration : rows * 1000,
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
  <!-- dummy tag and some blank lines for scrolling -->
  <span id="top"></span>
  <div
    id="dummy"
    style="position: absolute">
    <br />
    <c:forEach
      items="${allBracketInfo}"
      var="bracketInfo">

      <div class='center'>Head to Head Round
        ${bracketInfo.firstRound}, Head to Head Bracket
        ${bracketInfo.bracketName}</div>
      <br />
                        
   ${bracketInfo.displayBracketOutput}
     <c:if test="${allBracketInfo.size() > 1}">
       <br />
       <br />
       <hr />
       <br />
       <br />
     </c:if>
</c:forEach>

    <span id="bottom">&nbsp;</span>
  </div>

</body>
</html>

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
  src="<c:url value='/playoff/code.icepush'/>"></script>
<script
  type="text/javascript"
  src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script
  type="text/javascript"
  src="<c:url value='/extlib/jquery.scrollTo-2.1.2.min.js'/>"></script>
<script type="text/javascript">
  var ajaxURL = '<c:url value="/ajax/"/>';
  var numRows = parseInt("${numRows}"); // could be here directly as an intger, but the JSTL and auto-formatting don't agree
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

  var ajaxList;

  function iterate() {
    $.ajax({
      url : ajaxURL + "BracketQuery?multi=" + ajaxList,
      dataType : "json",
      cache : false,
      beforeSend : function(xhr) {
        xhr.overrideMimeType('text/plain');
      }
    }).done(
        function(mainData) {
          $.each(mainData, function(index, data) {
            var lid = data.originator;
            //First and foremost, make sure rounds haven't advanced and the division is the same.
            if (mainData.refresh == "true") {
              window.location.reload();
            } else {
              if (data.leaf.team) {
                if (data.leaf.team.teamNumber < 0) {
                  // internal teams 

                  if (data.leaf.team.teamNumber == -3) {
                    // NULL team number
                    return;
                  }
                  $("#" + lid).html(
                      displayStrings.getSpecialString(lid, data.leaf));
                  return;
                } else {
                  var score;
                  //table label?
                  placeTableLabel(lid, data.leaf.table, data.leaf.dbline);
                  var scoreData = data.score;
                  if (scoreData >= 0) {
                    $("#" + lid).html(
                        displayStrings.getTeamNameAndScoreString(lid,
                            data.leaf, scoreData));
                    return;
                  } else if (scoreData == -2) {
                    $("#" + lid).html(
                        displayStrings.getTeamNameAndScoreString(lid,
                            data.leaf, "No Show"));
                    return;
                  } else if (scoreData == -1) {
                    $("#" + lid).html(
                        displayStrings.getTeamNameString(lid, data.leaf));
                    return;
                  } // /else
                } // else if team num not a bye
              } // have a team number
            } // not refresh
          }); // each of data
          colorTableLabels();
        }).error(function(xhr, errstring, err) {
      window.location.reload();
      console.log(xhr);
      console.log(errstring);
      console.log(err);
    }); // /first .ajax
  } // /iterate()

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

  function buildAJAXList() {
    ajaxList = ""; // initialize to empty string

    $(".js-leaf").each(function() {
      if (typeof $(this).attr('id') == 'string') { // non-null id
        ajaxList = ajaxList + $(this).attr('id') + "|";
      }
    });

    //remove last pipe
    ajaxList = ajaxList.slice(0, ajaxList.length - 1);
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
    buildAJAXList();
    <c:if test="${empty param.scroll}">
    scrollToBottom();
    </c:if>
    colorTableLabels();
  });
</script>
<icep:register
  group="playoffs"
  callback="function(){iterate();}" />
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
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />
      <br />

      <div class='center'>Head to Head Round
        ${bracketInfo.firstRound}, Head to Head Bracket
        ${bracketInfo.bracketDivision}</div>
      <br />
                        
   ${bracketInfo.topRightBracketOutput}
</c:forEach>

    <span id="bottom">&nbsp;</span>
  </div>

</body>
</html>

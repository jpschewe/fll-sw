<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="fll.web.ApplicationAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List"%>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="fll.Team" %>
<%@ page import="fll.web.playoff.Playoff" %>

<%
	/*
  application parameters
  playoffDivision - String for the division
  playoffRoundNumber - Integer for the playoff round number, counted from the 1st playoff round
   */

   final DataSource datasource = ApplicationAttributes.getDataSource(application);
   final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);

  final String divisionKey = "playoffDivision";
  final String roundNumberKey = "playoffRoundNumber";
  final String displayName = SessionAttributes.getAttribute(session, "displayName", String.class);

  final String sessionDivision;
  final Number sessionRoundNumber;
  if (null != displayName) {
    sessionDivision = ApplicationAttributes.getAttribute(application, displayName
        + "_" + divisionKey, String.class);
    sessionRoundNumber = ApplicationAttributes.getAttribute(application, displayName
        + "_" + roundNumberKey, Number.class);
  } else {
    sessionDivision = null;
    sessionRoundNumber = null;
  }

  final String division;
  if (null != sessionDivision) {
    division = sessionDivision;
  } else if (null == application.getAttribute(divisionKey)) {
    final List<String> divisions = Playoff.getPlayoffDivisions(connection, currentTournament);
    if (!divisions.isEmpty()) {
      division = divisions.get(0);
    } else {
      throw new RuntimeException("No division specified and no divisions in the database!");
    }
  } else {
    division = ApplicationAttributes.getAttribute(application, divisionKey, String.class);
  }

  final int playoffRoundNumber;
  if (null != sessionRoundNumber) {
    playoffRoundNumber = sessionRoundNumber.intValue();
  } else if (null == application.getAttribute(roundNumberKey)) {
    playoffRoundNumber = 1;
  } else {
    playoffRoundNumber = ApplicationAttributes.getAttribute(application, roundNumberKey, Number.class).intValue();
  }

  final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection, division);

  final BracketData bracketInfo = new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber + 2, 4, false, true);

  bracketInfo.addBracketLabels(playoffRoundNumber);
  bracketInfo.addStaticTableLabels();
  
  pageContext.setAttribute("playoffRoundNumber", playoffRoundNumber);
  pageContext.setAttribute("numPlayoffRounds", numPlayoffRounds);
%>

<html>
<head>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
<link rel="stylesheet" type="text/css" href="<c:url value='/scoreboard/score_style.css'/>" />

<title>Playoff Round <%=playoffRoundNumber%>, Division <%=division%></title>
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
	padding-right: 5%
}
</style>
<script type="text/javascript" src="<c:url value='/playoff/code.icepush'/>"></script>
<script type="text/javascript" src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/extlib/jquery.scrollTo-2.1.2.min.js'/>"></script>
<script type="text/javascript">
  var ajaxURL = '<c:url value="/ajax/"/>';
  var currentRound = <%=playoffRoundNumber-1%>;
  var foundNewest = false;
  var rows = <%=bracketInfo.getNumRows()%>;
  var finalRound = <%=Queries.getNumPlayoffRounds(connection, division)+1%>;
  var maxNameLength = <%=Team.MAX_TEAM_NAME_LEN%>;
  var division = "<%=division%>";
  
  var displayStrings = new Object();
  displayStrings.parseTeamName = function (team) {
      if (team.length > maxNameLength) {
          return team.substring(0, maxNameLength-3)+"...";
      } else {
          return team;
      }
  }
  displayStrings.getSpecialString = function (id, data, newest) {
      if (newest) {
          return "<a name=\"newest\" id=\""+id+"-n\"></a><span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span>";
      } else {
          return "<span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span>";
      }
  }
  displayStrings.getTeamNameString = function (id, data, newest) {
      if (newest) {
          return "<a name=\"newest\" id=\""+id+"-n\"></a><span class=\"TeamNumber\">#" + data.team.teamNumber + "</span> <span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span>";
      } else {
          return "<span class=\"TeamNumber\">#" + data.team.teamNumber + "</span> <span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span>";
      }
  }
  displayStrings.getTeamNameAndScoreString = function (id, data, scoreData, newest) {
      if (scoreData != "No Show") {
          scoreData += ".0";
      }
      if (newest) {
          return "<a name=\"newest\" id=\""+id+"-n\"></a><span class=\"TeamNumber\">#" + data.team.teamNumber + "</span> <span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span><span class=\"TeamScore\"> Score: " + scoreData + "</span>";
      } else {
          return "<span class=\"TeamNumber\">#" + data.team.teamNumber + "</span> <span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span><span class=\"TeamScore\"> Score: " + scoreData + "</span>";
      }
  }

  var ajaxList;
  
  function iterate() {
      foundNewest = false;
      $("a[name=newest]").remove();
      $.ajax({
          url: ajaxURL + "BracketQuery?division=" + division + "&multi=" + ajaxList,
          dataType: "json",
          cache: false,
          beforeSend: function (xhr) {
              xhr.overrideMimeType('text/plain');
          }
      }).done(function (mainData) {
          $.each(mainData, function (index, data) {
              var lid = data.originator;
              //First and foremost, make sure rounds haven't advanced and the division is the same.
              if (mainData.refresh == "true") {
                  window.location.reload();
              }
              if (data.leaf.team.teamNumber < 0) {
                  if (data.leaf.team.teamNumber == -3) {
                      return;
                  }
                  if ($("#" + lid).html() != displayStrings.getSpecialString(lid, data.leaf, false) && !foundNewest) {
                      $("#" + lid).html(displayStrings.getSpecialString(lid, data.leaf, true));
                      foundNewest = true;
                  } else {
                      $("#" + lid).html(displayStrings.getSpecialString(lid, data.leaf, false));
                  }
                  return;
              } else if (lid.split("-")[1] != finalRound)/*Don't show final results!*/ { // /if team number meant a bye
                  var score;
                  //table label?
                  placeTableLabel(lid, data.leaf.table, data.leaf.dbline);
                  var scoreData = data.score;
                  if (scoreData >= 0) {
                      if ($("#" + lid).html() != displayStrings.getTeamNameAndScoreString(lid, data.leaf, scoreData, false) && !foundNewest) {
                          $("#" + lid).html(displayStrings.getTeamNameAndScoreString(lid, data.leaf, scoreData, true));
                          foundNewest = true;
                      } else {
                          $("#" + lid).html(displayStrings.getTeamNameAndScoreString(lid, data.leaf, scoreData, false));
                      }
                      return;
                  } else if (scoreData == -2) {
                      if ($("#" + lid).html() != displayStrings.getTeamNameAndScoreString(lid, data.leaf, "No Show", false) && !foundNewest) {
                          $("#" + lid).html(displayStrings.getTeamNameAndScoreString(lid, data.leaf, "No Show", true));
                          foundNewest = true;
                      } else {
                          $("#" + lid).html(displayStrings.getTeamNameAndScoreString(lid, data.leaf, "No Show", false));
                      }
                      return;
                  } else if (scoreData == -1) {
                      if ($("#" + lid).html() != displayStrings.getTeamNameString(lid, data.leaf, false) && !foundNewest) {
                          $("#" + lid).html(displayStrings.getTeamNameString(lid, data.leaf, true));
                          foundNewest = true;
                      } else {
                          $("#" + lid).html(displayStrings.getTeamNameString(lid, data.leaf, false));
                      }
                      return;
                  } // /else
                  //}); // /.done
              } // /if team num not a bye
          }); // /.each of data
          colorTableLabels();
      }).error(function (xhr, errstring, err) { 
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
          var row;
          //Are we on the top of a bracket or the bottom?
          if (dbLine % 2 == 1) {
              row = $("#" + lid).parent().next();
          } else {
              row = $("#" + lid).parent().prev();
          }
          //Selector is SUPPOSED to pick the nth cell with widths of 400, but I have to use this lazy switch to account for bridges, since it doesn't want to work.
          var nthcell = ((parseInt(lid.split("-")[1]) - currentRound));
          if (nthcell == 3) {
              nthcell = 4;
          } 
              row.find("td[width=\"400\"]:nth-child(" +  nthcell + ")").eq(0).css('padding-right', '30px').attr('align', 'right').html('<span class="table_assignment">' + table + '</span><!-- '+lid+' -->');
      }
  }

  function colorTableLabels() {
      $(".table_assignment").each(function(index, label) {
          //Sane color? Let's start by splitting the label text
          if ($.inArray(label.innerHTML.split(" ")[0].toLowerCase(), validColors) > 0) {
              label.style.borderColor = label.innerHTML.split(" ")[0];
              label.style.borderStyle = "solid";
          }
      });
  }

  function buildAJAXList() {
      $(".js-leaf").each(function () {
          if (typeof $(this).attr('id') == 'string') {
              ajaxList = ajaxList + $(this).attr('id') + "|";
          }
      });
      //remove last pipe
      ajaxList = ajaxList.slice(0, ajaxList.length - 1);
      ajaxList = ajaxList.replace(new RegExp("[a-z]", "g"), "");
  }

  function scrollToBottom() {   
    $.scrollTo($("#bottom"), {
      duration: rows * 1000,
      easing: 'linear',
      onAfter: scrollToTop,
  });    
  }
  
  function scrollToTop() {
    $.scrollTo($("#top"), {
      duration: rows * 1000,
      easing: 'linear',
      onAfter: scrollToBottom,
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
<icep:register group="playoffs" callback="function(){iterate();}"/>
</head>
<body>
<!-- dummy tag and some blank lines for scolling -->
<span id="top"></span>
<div id="dummy" style="position: absolute"><br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>

  <c:if test="${playoffRoundNumber <= numPlayoffRounds }">
   <%=bracketInfo.outputBrackets(BracketData.TopRightCornerStyle.MEET_TOP_OF_CELL)%>
  </c:if>
  
  <span id="bottom">&nbsp;</span>
</div>


</body>
</html>

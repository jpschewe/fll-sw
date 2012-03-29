<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List"%>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="fll.Team" %>

<%
	/*
  application parameters
  playoffDivision - String for the division
  playoffRoundNumber - Integer for the playoff round number, counted from the 1st playoff round
   */

   final DataSource datasource = SessionAttributes.getDataSource(session);
   final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);

  final String divisionKey = "playoffDivision";
  final String roundNumberKey = "playoffRoundNumber";
  final String displayName = (String)session.getAttribute("displayName");

  final String sessionDivision;
  final Number sessionRoundNumber;
  if (null != displayName) {
    sessionDivision = (String) application.getAttribute(displayName
        + "_" + divisionKey);
    sessionRoundNumber = (Number) application.getAttribute(displayName
        + "_" + roundNumberKey);
  } else {
    sessionDivision = null;
    sessionRoundNumber = null;
  }

  final String division;
  if (null != sessionDivision) {
    division = sessionDivision;
  } else if (null == application.getAttribute(divisionKey)) {
    final List<String> divisions = Queries.getEventDivisions(connection);
    if (!divisions.isEmpty()) {
      division = divisions.get(0);
    } else {
      throw new RuntimeException("No division specified and no divisions in the database!");
    }
  } else {
    division = (String) application.getAttribute(divisionKey);
  }

  final int playoffRoundNumber;
  if (null != sessionRoundNumber) {
    playoffRoundNumber = sessionRoundNumber.intValue();
  } else if (null == application.getAttribute(roundNumberKey)) {
    playoffRoundNumber = 1;
  } else {
    playoffRoundNumber = ((Number) application.getAttribute(roundNumberKey)).intValue();
  }

  final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection, division);

  final BracketData bracketInfo = new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber + 2, 4, false, true);

  bracketInfo.addBracketLabels(playoffRoundNumber);
  bracketInfo.addStaticTableLabels(connection, currentTournament, division);
%>

<html>
<head>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
<title>Playoff Round <%=playoffRoundNumber%>, Division <%=division%></title>
<style type='text/css'>
TD.Leaf {
	color: #ffffff;
	font-family: Arial;
	background-color: #000000
}

TD.Bridge {
	background-color: #808080
}

FONT {
	font-family: Arial
}

FONT.TeamNumber {
	color: #ff8080;
	font-weight: bold
}

FONT.TeamName {
	color: #ffffff;
	font-weight: bold
}

FONT.TeamScore {
	color: #ffffff;
	font-weight: bold;
	font-size: 10pt
}

FONT.TIE {
	color: #ff0000;
	font-weight: bold
}

.TABLE_ASSIGNMENT {
	font-family: monospace;
	font-size: 85%;
	font-weight: bold;
	background-color: white;
	padding-left: 5%;
	padding-right: 5%
}
</style>
<script type="text/javascript" src="<c:url value='/playoff/code.icepush'/>"></script>
<script type="text/javascript" src="<c:url value='/extlib/jquery-1.7.1.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/extlib/jquery.scrollTo-1.4.2-min.js'/>"></script>
<script type="text/javascript">
  var ajaxURL = '<c:url value="/ajax/"/>';
  var seedingRounds = <%=Queries.getNumSeedingRounds(connection, currentTournament)%>;
  var currentRound = <%=playoffRoundNumber-1%>;
  var foundNewest = false;
  var rows = <%=bracketInfo.getNumRows()%>;
  var finalRound = <%=Queries.getNumPlayoffRounds(connection, division)+1%>;
  var maxNameLength = <%=Team.MAX_TEAM_NAME_LEN%>;
  
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
          return "<a name=\"newest\" id=\""+id+"-n\"></a><font class=\"TeamName\">" + displayStrings.parseTeamName(data._team._teamName) + "</font>";
      } else {
          return "<font class=\"TeamName\">" + displayStrings.parseTeamName(data._team._teamName) + "</font>";
      }
  }
  displayStrings.getTeamNameString = function (id, data, newest) {
      if (newest) {
          return "<a name=\"newest\" id=\""+id+"-n\"></a><font class=\"TeamNumber\">#" + data._team._teamNumber + "</font> <font class=\"TeamName\">" + displayStrings.parseTeamName(data._team._teamName) + "</font>";
      } else {
          return "<font class=\"TeamNumber\">#" + data._team._teamNumber + "</font> <font class=\"TeamName\">" + displayStrings.parseTeamName(data._team._teamName) + "</font>";
      }
  }
  displayStrings.getTeamNameAndScoreString = function (id, data, scoreData, newest) {
      if (scoreData != "No Show") {
          scoreData += ".0";
      }
      if (newest) {
          return "<a name=\"newest\" id=\""+id+"-n\"></a><font class=\"TeamNumber\">#" + data._team._teamNumber + "</font> <font class=\"TeamName\">" + displayStrings.parseTeamName(data._team._teamName) + "</font><font class=\"TeamScore\"> Score: " + scoreData + "</font>";
      } else {
          return "<font class=\"TeamNumber\">#" + data._team._teamNumber + "</font> <font class=\"TeamName\">" + displayStrings.parseTeamName(data._team._teamName) + "</font><font class=\"TeamScore\"> Score: " + scoreData + "</font>";
      }
  }

  var ajaxList;
  var ajaxArray;

  function iterate() {
      foundNewest = false;
      $("a[name=newest]").remove();
      $.ajax({
          url: ajaxURL + "BracketQuery?multi=" + ajaxList,
          dataType: "json",
          cache: false,
          beforeSend: function (xhr) {
              xhr.overrideMimeType('text/plain');
          }
      }).done(function (mainData) {
          $.each(mainData, function (index, data) {
              var lid = data.originator;
              //First and foremost, make sure rounds haven't advanced.
              if (mainData.refresh == "true") {
                  window.location.reload();
              }
              if (data.leaf._team._teamNumber < 0) {
                  if (data.leaf._team._teamNumber == -3) {
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
                  placeTableLabel(lid, data.leaf._table, data.leaf._dbLine);
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
              label.style.backgroundColor = label.innerHTML.split(" ")[0];
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

  function scrollMgr(nexttgt) {
      if (nexttgt == 'top') {
          scrollTimer = $.scrollTo($("#top"), rows * 1000, {
              easing: 'linear'
          });
          scrollTimer = $.scrollTo($("#top"), 3000);
      } else if (nexttgt == 'bottom') {
          scrollTimer = $.scrollTo($("#bottom"), rows * 1000, {
              easing: 'linear'
          });
          scrollTimer = $.scrollTo($("#bottom"), 3000);
      }
  }

  $(document).ready(function() {
      buildAJAXList();
      <c:if test="${empty param.scroll}">
      scrollMgr("bottom");
      scrollMgr("top");
      window.setInterval('scrollMgr("bottom");scrollMgr("top")', (rows * 2000)+6000);
      </c:if>
      colorTableLabels();
      //window.setInterval('iterate()',10000);
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
<table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>
 <%
   if (playoffRoundNumber <= numPlayoffRounds) {

     for (int rowIndex = 1; rowIndex <= bracketInfo.getNumRows(); rowIndex++) {
 %>
 <tr>
  <%
    // Get each cell. Insert bridge cells between columns.
        for (int i = bracketInfo.getFirstRound(); i < bracketInfo.getLastRound(); i++) {
  %>
  <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, i)%>
  <%=bracketInfo.getHtmlBridgeCell(rowIndex, i, BracketData.TopRightCornerStyle.MEET_TOP_OF_CELL)%>
  <%
    }
  %>
  <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, bracketInfo.getLastRound())%>
 </tr>
 <%
   }
   }//end if we have more than 2 teams
 %>
</table>
<span id="bottom">&nbsp;</span>
</div>


</body>
</html>

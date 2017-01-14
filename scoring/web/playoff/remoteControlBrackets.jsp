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
<%@ page import="fll.web.DisplayInfo" %>

<%
  /*
  application parameters
  playoffDivision - String for the division
  playoffRoundNumber - Integer for the playoff round number, counted from the 1st playoff round
   */

   final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);
      // store the brackets to know when a refresh is required
      session.setAttribute("brackets", displayInfo.getBrackets());
      
      final DisplayInfo.H2HBracketDisplay h2hBracket = displayInfo.getBrackets().get(0); //HACK just for now
      
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

  final String division = h2hBracket.getBracket();

  final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection, division);

  final BracketData bracketInfo = new BracketData(connection, h2hBracket.getBracket(), h2hBracket.getFirstRound(), h2hBracket.getFirstRound() + 2, 4, false, true);

  bracketInfo.addBracketLabels(h2hBracket.getFirstRound());
  bracketInfo.addStaticTableLabels();
  
  pageContext.setAttribute("playoffRoundNumber", h2hBracket.getFirstRound());
  pageContext.setAttribute("numPlayoffRounds", numPlayoffRounds); // used to limit when the output function is called, could probably be handled in said function
  pageContext.setAttribute("bracketName", h2hBracket.getBracket());
  pageContext.setAttribute("maxNameLength", Team.MAX_TEAM_NAME_LEN);
  pageContext.setAttribute("numRows", bracketInfo.getNumRows()); // needs to be the sum of numRows for all brackets
%>

<html>
<head>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/fll-sw.css'/>" />
<link rel="stylesheet" type="text/css" href="<c:url value='/scoreboard/score_style.css'/>" />

<title>Head to Head Round ${playoffRoundNumber}, Head to Head Bracket ${bracket}</title>
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
<script type="text/javascript" src="<c:url value='/playoff/code.icepush'/>"></script>
<script type="text/javascript" src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/extlib/jquery.scrollTo-2.1.2.min.js'/>"></script>
<script type="text/javascript">
  var ajaxURL = '<c:url value="/ajax/"/>';
  var rows = ${numRows}; // need total number of rows for scrolling
  var maxNameLength = ${maxNameLength};
  
  var displayStrings = new Object();
  displayStrings.parseTeamName = function (team) {
      if (team.length > maxNameLength) {
          return team.substring(0, maxNameLength-3)+"...";
      } else {
          return team;
      }
  }
  displayStrings.getSpecialString = function (id, data) {
      return "<span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span>";
  }
  displayStrings.getTeamNameString = function (id, data) {
      return "<span class=\"TeamNumber\">#" + data.team.teamNumber + "</span> <span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span>";
  }
  displayStrings.getTeamNameAndScoreString = function (id, data, scoreData) {
      if (scoreData != "No Show") {
          scoreData += ".0";
      }
      return "<span class=\"TeamNumber\">#" + data.team.teamNumber + "</span> <span class=\"TeamName\">" + displayStrings.parseTeamName(data.team.teamName) + "</span><span class=\"TeamScore\"> Score: " + scoreData + "</span>";
  }

  var ajaxList;
  
  function iterate() {
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
              //First and foremost, make sure rounds haven't advanced and the division is the same.
              if (mainData.refresh == "true") {
                  window.location.reload();
              } else {
                  if(data.leaf.team) {
                      if (data.leaf.team.teamNumber < 0) {
                        // internal teams 
                        
                          if (data.leaf.team.teamNumber == -3) {
                            // NULL team number
                              return;
                          }
                          $("#" + lid).html(displayStrings.getSpecialString(lid, data.leaf));
                          return;
                      } else {
                          var score;
                          //table label?
                          placeTableLabel(lid, data.leaf.table, data.leaf.dbline);
                          var scoreData = data.score;
                          if (scoreData >= 0) {
                              $("#" + lid).html(displayStrings.getTeamNameAndScoreString(lid, data.leaf, scoreData));
                              return;
                          } else if (scoreData == -2) {
                              $("#" + lid).html(displayStrings.getTeamNameAndScoreString(lid, data.leaf, "No Show"));
                              return;
                          } else if (scoreData == -1) {
                              $("#" + lid).html(displayStrings.getTeamNameString(lid, data.leaf));
                              return;
                          } // /else
                      } // else if team num not a bye
                  } // have a team number
              } // not refresh
          }); // each of data
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
          $("#" + lid + "-table").text(table);
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
      ajaxList = ""; // initialize to empty string
      
      $(".js-leaf").each(function () {
          if (typeof $(this).attr('id') == 'string') { // non-null id
              ajaxList = ajaxList + $(this).attr('id') + "|";
          }
      });

      //remove last pipe
      ajaxList = ajaxList.slice(0, ajaxList.length - 1);
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

<div class='center'>Head to Head Bracket ${bracketName}</div>
                        <br/>
                        
   <%=bracketInfo.outputBrackets(BracketData.TopRightCornerStyle.MEET_TOP_OF_CELL)%>

  <span id="bottom">&nbsp;</span>
</div>


</body>
</html>

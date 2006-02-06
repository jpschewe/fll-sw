<%@ include file="/WEB-INF/jspf/init_pdf.jspf"      
%><%@ page import="fll.Team"
%><%@ page import="fll.pdf.scoreEntry.ScoresheetGenerator"
%><%@ page import="fll.Queries"
%><%@ page import="fll.Utilities"
%><%@ page import="fll.web.playoff.Playoff"
%><%@ page import="fll.web.scoreEntry.ScoreEntry"
  
%><%@ page import="org.w3c.dom.Document"
%><%@ page import="org.w3c.dom.Element"
  
%><%@ page import="java.text.NumberFormat"
%><%@ page import="java.util.Map"

%><%@ page import="java.sql.Connection"
  
%><%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

// Create the scoresheet generator - must provide correct number of scoresheets
ScoresheetGenerator scoresheetGen = new ScoresheetGenerator(request.getParameterMap(), challengeDocument);

// Write the scoresheets to the browser - content-type: application/pdf
scoresheetGen.writeFile(response.getOutputStream());

%>
<%@ include file="/WEB-INF/jspf/init_pdf.jspf"
%><%@ page import="fll.pdf.report.FinalComputedScores"
%><%@ page import="fll.Queries"
%><%@ page import="java.sql.Connection"
%><%@ page import="org.w3c.dom.Document"
%><%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
final String tournamentReq = request.getParameter("currentTournament");
final String tournament;
if(tournamentReq == null) {
  tournament = Queries.getCurrentTournament(connection);
} else {
  tournament = tournamentReq;
}

FinalComputedScores fcs = new FinalComputedScores(challengeDocument, tournament);

fcs.generateReport(challengeDocument, connection, response.getOutputStream());
%>
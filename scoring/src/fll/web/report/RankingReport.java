/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import fll.CategoryRank;
import fll.TeamRanking;
import fll.TournamentTeam;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;

/**
 * @author jpschewe
 */
@WebServlet("/report/RankingReport")
public class RankingReport extends BaseFLLServlet {

  private static final Logger LOG = LogUtils.getLogger();

  private static final Font RANK_TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);

  private static final Font RANK_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);

  private static final Font HEADER_FONT = TITLE_FONT;

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/RankingReport")) {
      return;
    }

    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      final boolean useQuartiles = GlobalParameters.getUseQuartilesInRankingReport(connection);

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      // create simple doc and write to a ByteArrayOutputStream
      final Document document = new Document(PageSize.LETTER);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final PdfWriter writer = PdfWriter.getInstance(document, baos);
      writer.setPageEvent(new ReportPageEventHandler(HEADER_FONT, "Final Computed Rankings",
                                                     challengeDescription.getTitle(),
                                                     Queries.getCurrentTournamentName(connection)));

      document.open();

      document.addTitle("Ranking Report");

      // add content
      final Map<Integer, TeamRanking> teamRankings = Queries.getTeamRankings(connection, challengeDescription);
      final List<Integer> teamNumbers = new LinkedList<Integer>(teamRankings.keySet());
      Collections.sort(teamNumbers);
      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection);

      for (final int teamNum : teamNumbers) {
        final TournamentTeam team = tournamentTeams.get(teamNum);
        final Paragraph para = new Paragraph();
        para.add(Chunk.NEWLINE);
        para.add(new Chunk("Ranks for Team "
            + teamNum, TITLE_FONT));
        para.add(Chunk.NEWLINE);
        para.add(new Chunk(team.getTeamName()
            + " / " + team.getOrganization(), TITLE_FONT));
        para.add(Chunk.NEWLINE);
        para.add(new Chunk("Award Group: "
            + team.getAwardGroup(), TITLE_FONT));
        para.add(Chunk.NEWLINE);
        para.add(new Chunk(
                           "Each team is ranked in each category in the judging group and award group they were judged in. Performance scores are ranked by award group only. Teams may have the same rank if they were tied.",
                           RANK_VALUE_FONT));
        para.add(Chunk.NEWLINE);
        para.add(Chunk.NEWLINE);
        final TeamRanking teamRanks = teamRankings.get(teamNum);

        final List<String> categories = teamRanks.getCategories();
        Collections.sort(categories);

        // pull out performance next
        if (categories.contains(CategoryRank.PERFORMANCE_CATEGORY_NAME)) {
          final String category = CategoryRank.PERFORMANCE_CATEGORY_NAME;
          outputCategory(para, teamRanks, category, useQuartiles);
        }
        para.add(Chunk.NEWLINE);

        for (final String category : categories) {
          if (!CategoryRank.PERFORMANCE_CATEGORY_NAME.equals(category)
              && !CategoryRank.OVERALL_CATEGORY_NAME.equals(category)) {
            outputCategory(para, teamRanks, category, useQuartiles);
          }
        }

        document.add(para);

        final Paragraph definitionPara = new Paragraph();
        definitionPara.add(Chunk.NEWLINE);
        definitionPara.add(new Chunk(
                                     "The 1st quartile is the top 25% of teams, 2nd quartile is the next 25%, quartiles 3 and 4 are the following 25% groupings of teams."));
        document.add(definitionPara);

        document.add(Chunk.NEXTPAGE);
      }

      document.close();

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=rankingReport.pdf");
      // the content length is needed for MSIE!!!
      response.setContentLength(baos.size());
      // write ByteArrayOutputStream to the ServletOutputStream
      final ServletOutputStream out = response.getOutputStream();
      baos.writeTo(out);
      out.flush();

    } catch (final SQLException e) {
      LOG.error(e, e);
      throw new RuntimeException(e);
    } catch (final DocumentException e) {
      LOG.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  private void outputCategory(final Paragraph para,
                              final TeamRanking teamRanks,
                              final String category,
                              boolean useQuartiles) {
    para.add(new Chunk(category
        + ": ", RANK_TITLE_FONT));

    final CategoryRank catRank = teamRanks.getRankForCategory(category);

    final int rank = catRank.getRank();
    if (CategoryRank.NO_SHOW_RANK == rank) {
      para.add(new Chunk("No Show", RANK_VALUE_FONT));
    } else {
      final double percentage = (double) rank
          / catRank.getNumTeams();
      if (useQuartiles) {
        para.add(new Chunk(String.format("%s in %s", convertPercentageToQuartile(percentage), catRank.getGroup()),
                           RANK_VALUE_FONT));
      } else {
        para.add(new Chunk(String.format("%d out of %d teams in %s", rank, catRank.getNumTeams(), catRank.getGroup()),
                           RANK_VALUE_FONT));
      }
    }
    para.add(Chunk.NEWLINE);
  }

  private static String convertPercentageToQuartile(final double percentage) {
    if (percentage <= 0.25) {
      return "Quartile 1";
    } else if (percentage <= 0.5) {
      return "Quartile 2";
    } else if (percentage <= 0.75) {
      return "Quartile 3";
    } else {
      return "Quartile 4";
    }
  }
}

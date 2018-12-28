/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.SchemaAnalyzer;
import net.sourceforge.schemaspy.model.InvalidConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.util.LogUtils;
import fll.xml.ChallengeParser;

/**
 * Generate a schema diagram of our example database.
 */
public class GenerateDatabaseDiagram {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param args
   */
  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    if (args.length != 1) {
      LOGGER.fatal("You must specify the output directory");
      return;
    }

    final String dbname = "generate_schema";
    Connection connection = null;
    try {
      // generate example database
      final DataSource datasource = Utilities.createMemoryDataSource(dbname);
      connection = datasource.getConnection();

      final String baseDir = "fll/resources/challenge-descriptors/";
      final String challengeName = "example-database.xml";
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      final URL challengeUrl = classLoader.getResource(baseDir
          + challengeName);

      final InputStream stream = challengeUrl.openStream();
      final Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET);
      final Document document = ChallengeParser.parse(reader);

      GenerateDB.generateDB(document, connection);

      SchemaAnalyzer analyzer = new SchemaAnalyzer();
      final Config config = new HsqlMemConfig();
      config.setAdsEnabled(false);
      config.setDb(dbname);
      config.setHighQuality(true);
      config.setSchema("PUBLIC");
      config.setUser("SA");
      config.setOutputDir(args[0]);
      analyzer.analyze(config);

    } catch (final SQLException e) {
      LOGGER.fatal("Error talking to the database", e);
    } catch (final Exception e) {
      LOGGER.fatal("Error creating the diagram", e);
    } finally {
      // clean up database
      SQLFunctions.close(connection);
    }

  }

  /**
   * Special {@link Config} for handling hsqldb mem. Also turns off the SF logo.
   */
  private static final class HsqlMemConfig extends Config {
    @Override
    public String getDbType() {
      // make sure we are compatible with the existing configurations
      return "hsqldb";
    }

    @Override
    public String getHost() {
      // keeps DbSpecificConfig for hsql happy
      return "localhost";
    }

    @Override
    public Properties getDbProperties(final String type) throws IOException, InvalidConfigurationException {
      final Properties props = super.getDbProperties("hsqldb");
      props.setProperty("connectionSpec", "jdbc:hsqldb:mem:<db>");
      return props;
    }

    /**
     * Always disable the logo.
     */
    @Override
    public boolean isLogoEnabled() {
      return false;
    }

  }

}

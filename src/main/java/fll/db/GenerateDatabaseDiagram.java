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

import fll.Utilities;
import fll.util.FLLInternalException;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import net.sourceforge.schemaspy.Config;
import net.sourceforge.schemaspy.SchemaAnalyzer;
import net.sourceforge.schemaspy.model.InvalidConfigurationException;

/**
 * Generate a schema diagram of our example database.
 */
public final class GenerateDatabaseDiagram {

  private GenerateDatabaseDiagram() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param args output directory
   */
  public static void main(final String[] args) {
    if (args.length != 1) {
      LOGGER.fatal("You must specify the output directory");
      return;
    }

    final String dbname = "generate_schema";
    final DataSource datasource = Utilities.createMemoryDataSource(dbname);

    try (Connection connection = datasource.getConnection()) {

      final String baseDir = "fll/resources/challenge-descriptors/";
      final String challengeName = "example-database.xml";
      final ClassLoader classLoader = getClassLoader();
      final URL challengeUrl = classLoader.getResource(baseDir
          + challengeName);
      if (null == challengeUrl) {
        throw new FLLInternalException("Cannot find example-database.xml");
      }

      final InputStream stream = challengeUrl.openStream();
      final Reader reader = new InputStreamReader(stream, Utilities.DEFAULT_CHARSET);
      final ChallengeDescription description = ChallengeParser.parse(reader);

      GenerateDB.generateDB(description, connection);

      final SchemaAnalyzer analyzer = new SchemaAnalyzer();
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
    }

  }

  private static ClassLoader getClassLoader() {
    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    if (null != contextClassLoader) {
      return contextClassLoader;
    }

    final ClassLoader classClassLoader = GenerateDatabaseDiagram.getClassLoader();
    if (null != classClassLoader) {
      return classClassLoader;
    }

    return ClassLoader.getSystemClassLoader();
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

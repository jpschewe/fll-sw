/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.UploadFileSpec;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Some utilities for writing tests.
 * 
 * @version $Revision$
 */
public final class TestUtils {

  private static final Logger LOG = Logger.getLogger(TestUtils.class);

  /**
   * Root URL for the software with trailing slash.
   */
  public static final String URL_ROOT = "http://localhost:9080/fll-sw/";

  private TestUtils() {
    // no instances
  }

  /**
   * Creates a database connection to the test database started on localhost
   * inside tomcat.
   * 
   * @param username username to use
   * @param password password to use
   * @throws RuntimeException on an error
   */
  public static Connection createTestDBConnection() throws RuntimeException {
    // create connection to database and puke if anything goes wrong
    // register the driver
    try {
      Class.forName("org.hsqldb.jdbcDriver").newInstance();
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("Unable to load driver.", e);
    } catch (final InstantiationException ie) {
      throw new RuntimeException("Unable to load driver.", ie);
    } catch (final IllegalAccessException iae) {
      throw new RuntimeException("Unable to load driver.", iae);
    }

    Connection connection = null;
    final String myURL = "jdbc:hsqldb:hsql://localhost:9042/fll";
    if (LOG.isDebugEnabled()) {
      LOG.debug("created test database connection myURL: "
          + myURL);
    }
    try {
      connection = DriverManager.getConnection(myURL);
    } catch (final SQLException sqle) {
      throw new RuntimeException("Unable to create connection: "
          + sqle.getMessage() + " URL: " + myURL);
    }

    return connection;
  }

  /**
   * Initialize a database.
   * 
   * @param challengeDocIS input stream that has the tournament descriptor to
   *          load in it, this input stream is closed by this method upon
   *          successful completion
   * @throws SAXException
   * @throws IOException
   * @throws MalformedURLException
   */
  public static void initializeDatabase(final InputStream challengeDocIS) throws MalformedURLException, IOException, SAXException {
    Assert.assertNotNull("Challenge descriptor must not be null", challengeDocIS);

    final WebConversation conversation = new WebConversation();
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "setup/");
    WebResponse response = conversation.getResponse(request);
    Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

    final WebForm form = response.getFormWithID("setup");
    Assert.assertNotNull(form);
    form.setCheckbox("force_rebuild", true); // rebuild the whole database
    final UploadFileSpec challengeUpload = new UploadFileSpec("challenge-test.xml", challengeDocIS, "text/xml");
    Assert.assertNotNull(challengeUpload);
    form.setParameter("xmldocument", new UploadFileSpec[] { challengeUpload });
    request = form.getRequest("reinitializeDatabase");
    response = conversation.getResponse(request);
    Assert.assertTrue(response.isHTML());
    Assert.assertNotNull("Error initializing database: "
        + response.getText(), response.getElementWithID("success"));
    challengeDocIS.close();
  }

  /**
   * Initialize a database from a zip file.
   * 
   * @param inputStream input stream that has database to load in it, this input
   *          stream is closed by this method upon successful completion
   * @throws SAXException
   * @throws IOException
   * @throws MalformedURLException
   */
  public static void initializeDatabaseFromDump(final InputStream inputStream) throws MalformedURLException, IOException, SAXException {
    Assert.assertNotNull("Zip to load must not be null", inputStream);

    final WebConversation conversation = new WebConversation();
    WebRequest request = new GetMethodWebRequest(TestUtils.URL_ROOT
        + "setup/");
    WebResponse response = conversation.getResponse(request);
    Assert.assertTrue("Received non-HTML response from web server", response.isHTML());

    final WebForm form = response.getFormWithID("import");
    Assert.assertNotNull(form);
    final UploadFileSpec challengeUpload = new UploadFileSpec("database.zip", inputStream, "application/zip");
    Assert.assertNotNull(challengeUpload);
    form.setParameter("dbdump", new UploadFileSpec[] { challengeUpload });
    request = form.getRequest("createdb");
    response = conversation.getResponse(request);
    Assert.assertTrue(response.isHTML());
    Assert.assertNotNull("Error initializing database: "
        + response.getText(), response.getElementWithID("success"));
    inputStream.close();
  }

  /**
   * Delete all files that would be associated with the specified database. 
   */
  public static void deleteDatabase(final String database) {
    for(final String extension : Utilities.HSQL_DB_EXTENSIONS) {
      final String filename = database + extension;
      final File file = new File(filename);
      if(file.exists()) {
        if(!file.delete()) {
          file.deleteOnExit();
        }
      }
    }
  }
}

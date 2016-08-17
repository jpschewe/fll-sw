/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.TestUtils;
import fll.Tournament;
import fll.util.FLLInternalException;
import fll.util.LogUtils;
import fll.web.api.TournamentsServlet;
import fll.xml.BracketSortType;

/**
 * Some utilities for integration tests.
 */
public final class IntegrationTestUtils {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final String WAIT_FOR_PAGE_TIMEOUT = "60000";

  public static final String TEST_USERNAME = "fll";

  public static final String TEST_PASSWORD = "Lego";

  private IntegrationTestUtils() {
    // no instances
  }

  /**
   * Check if an element exists.
   */
  public static boolean isElementPresent(final WebDriver selenium,
                                         final By search) {
    boolean elementFound = false;
    try {
      selenium.findElement(search);
      elementFound = true;
    } catch (NoSuchElementException e) {
      elementFound = false;
    }
    return elementFound;
  }

  /**
   * Load a page and check to make sure the page didn't crash.
   * 
   * @param selenium the test controller
   * @param url the page to load
   * @throws IOException
   */
  public static void loadPage(final WebDriver selenium,
                              final String url)
      throws IOException {
    try {
      selenium.get(url);

      assertNoException(selenium);
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  public static void assertNoException(final WebDriver selenium) {
    Assert.assertFalse("Error loading page", isElementPresent(selenium, By.id("exception-handler")));
  }

  /**
   * Initialize the database using the given challenge descriptor.
   * 
   * @param driver the test controller
   * @param challengeStream the challenge descriptor
   * @throws IOException
   */
  public static void initializeDatabase(final WebDriver driver,
                                        final InputStream challengeStream)
      throws IOException {
    try {
      Assert.assertNotNull(challengeStream);
      final File challengeFile = IntegrationTestUtils.storeInputStreamToFile(challengeStream);
      try {
        driver.get(TestUtils.URL_ROOT
            + "setup/");

        if (isElementPresent(driver, By.name("submit_login"))) {
          login(driver);

          driver.get(TestUtils.URL_ROOT
              + "setup/");
        }

        final WebElement fileEle = driver.findElement(By.name("xmldocument"));
        fileEle.sendKeys(challengeFile.getAbsolutePath());

        final WebElement reinitDB = driver.findElement(By.name("reinitializeDatabase"));
        reinitDB.click();

        try {
          final Alert confirmCreateDB = driver.switchTo().alert();
          LOGGER.info("Confirmation text: "
              + confirmCreateDB.getText());
          confirmCreateDB.accept();
        } catch (final NoAlertPresentException e) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("No alert found, assuming the database was empty and didn't need an alert.");
          }
        }

        driver.findElement(By.id("success"));

        // setup user
        final WebElement userElement = driver.findElement(By.name("user"));
        userElement.sendKeys(TEST_USERNAME);

        final WebElement passElement = driver.findElement(By.name("pass"));
        passElement.sendKeys(TEST_PASSWORD);

        final WebElement passCheckElement = driver.findElement(By.name("pass_check"));
        passCheckElement.sendKeys(TEST_PASSWORD);

        final WebElement submitElement = driver.findElement(By.name("submit_create_user"));
        submitElement.click();

        driver.findElement(By.id("success-create-user"));

        login(driver);
      } finally {
        if (!challengeFile.delete()) {
          challengeFile.deleteOnExit();
        }
      }
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(driver);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(driver);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(driver);
      throw e;
    }
  }

  /**
   * Initialize a database from a zip file.
   * 
   * @param selenium the test controller
   * @param inputStream input stream that has database to load in it, this input
   *          stream is closed by this method upon successful completion
   * @throws IOException
   */
  public static void initializeDatabaseFromDump(final WebDriver selenium,
                                                final InputStream inputStream)
      throws IOException {
    try {
      Assert.assertNotNull(inputStream);
      final File dumpFile = IntegrationTestUtils.storeInputStreamToFile(inputStream);
      try {
        selenium.get(TestUtils.URL_ROOT
            + "setup/");

        if (isElementPresent(selenium, By.name("submit_login"))) {
          login(selenium);

          selenium.get(TestUtils.URL_ROOT
              + "setup/");
        }

        final WebElement dbEle = selenium.findElement(By.name("dbdump"));
        dbEle.sendKeys(dumpFile.getAbsolutePath());

        final WebElement createEle = selenium.findElement(By.name("createdb"));
        createEle.click();

        try {
          final Alert confirmCreateDB = selenium.switchTo().alert();
          LOGGER.info("Confirmation text: "
              + confirmCreateDB.getText());
          confirmCreateDB.accept();
        } catch (final NoAlertPresentException e) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("No alert found, assuming the database was empty and didn't need an alert.");
          }
        }

        selenium.findElement(By.id("success"));

        // setup user
        final WebElement userElement = selenium.findElement(By.name("user"));
        userElement.sendKeys(TEST_USERNAME);

        final WebElement passElement = selenium.findElement(By.name("pass"));
        passElement.sendKeys(TEST_PASSWORD);

        final WebElement passCheckElement = selenium.findElement(By.name("pass_check"));
        passCheckElement.sendKeys(TEST_PASSWORD);

        final WebElement submitElement = selenium.findElement(By.name("submit_create_user"));
        submitElement.click();

        selenium.findElement(By.id("success-create-user"));

        login(selenium);
      } finally {
        if (!dumpFile.delete()) {
          dumpFile.deleteOnExit();
        }
      }
      login(selenium);
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  /**
   * Defaults filePrefix to "fll".
   * 
   * @see #storeScreenshot(String, WebDriver)
   */
  public static void storeScreenshot(final WebDriver driver) throws IOException {
    storeScreenshot("fll", driver);
  }

  /**
   * Store screenshot and other information for debugging the error.
   * 
   * @param filePrefix prefix for the files that are created
   * @param driver
   * @throws IOException
   */
  public static void storeScreenshot(final String filePrefix,
                                     final WebDriver driver)
      throws IOException {
    final Path tempDir = Files.createTempDirectory(Paths.get("screenshots"), filePrefix);

    if (driver instanceof TakesScreenshot) {
      final Path screenshot = tempDir.resolve("screenshot.png");

      final File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
      Files.copy(scrFile.toPath(), screenshot);
      LOGGER.info("Screenshot saved to "
          + screenshot.toAbsolutePath().toString());
    } else {
      LOGGER.warn("Unable to get screenshot");
    }

    final Path htmlFile = tempDir.resolve("page.html");

    final String html = driver.getPageSource();
    final BufferedWriter writer = Files.newBufferedWriter(htmlFile);
    writer.write(html);
    writer.close();
    LOGGER.info("HTML saved to "
        + htmlFile.toAbsolutePath().toString());

    // get the database
    final Path dbroot = Paths.get("tomcat", "webapps", "fll-sw", "WEB-INF");
    LOGGER.info("Copying database files from "
        + dbroot.toAbsolutePath() + " to " + tempDir.toAbsolutePath());
    try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dbroot, "flldb*")) {
      for (final Path entry : stream) {
        if (Files.isRegularFile(entry)) {
          Files.copy(entry, tempDir.resolve(dbroot.relativize(entry)));
          LOGGER.info("Copied database file "
              + entry.toString());
        }
      }
    } catch (final DirectoryIteratorException ex) {
      LOGGER.error("Unable to get database files", ex);
    }
    LOGGER.info("Finished copying database files");

  }

  /**
   * Copy the contents of a stream to a temporary file.
   * 
   * @param inputStream the data to store in the temporary file
   * @return the temporary file, you need to delete it
   * @throws IOException
   */
  public static File storeInputStreamToFile(final InputStream inputStream) throws IOException {
    final File tempFile = File.createTempFile("fll", null);
    final FileOutputStream outputStream = new FileOutputStream(tempFile);
    final byte[] buffer = new byte[1042];
    int bytesRead;
    while (-1 != (bytesRead = inputStream.read(buffer))) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.close();

    return tempFile;
  }

  /**
   * Login to fll
   */
  public static void login(final WebDriver driver) {
    driver.get(TestUtils.URL_ROOT
        + "login.jsp");

    final WebElement userElement = driver.findElement(By.name("user"));
    userElement.sendKeys(TEST_USERNAME);

    final WebElement passElement = driver.findElement(By.name("pass"));
    passElement.sendKeys(TEST_PASSWORD);

    final WebElement submitElement = driver.findElement(By.name("submit_login"));
    submitElement.click();
  }

  private static String readAll(final Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  private static String readJSON(final String url) throws MalformedURLException, IOException {
    InputStream is = new URL(url).openStream();
    try {
      final BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
      final String jsonText = readAll(rd);
      return jsonText;
    } finally {
      is.close();
    }
  }

  /**
   * Find a tournament by name using the JSON API.
   * 
   * @param selenium web interface
   * @param tournamentName name of tournament
   * @return the tournament or null if not found
   */
  public static Tournament getTournamentByName(final WebDriver selenium,
                                               final String tournamentName)
      throws IOException {
    try {
      final String json = readJSON(TestUtils.URL_ROOT
          + "api/Tournaments");

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Tournaments json: "
            + json);
      }

      // get the JSON
      final ObjectMapper jsonMapper = new ObjectMapper();
      final Reader reader = new StringReader(json);

      final Collection<Tournament> tournaments = jsonMapper.readValue(reader,
                                                                      TournamentsServlet.TournamentsTypeInformation.INSTANCE);

      for (final Tournament tournament : tournaments) {
        if (tournament.getName().equals(tournamentName)) {
          return tournament;
        }
      }

      return null;

    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  /**
   * Add a team to a tournament.
   */
  public static void addTeam(final WebDriver selenium,
                             final int teamNumber,
                             final String teamName,
                             final String organization,
                             final String division,
                             final String tournamentName)
      throws IOException {
    try {
      final Tournament tournament = getTournamentByName(selenium, tournamentName);

      loadPage(selenium, TestUtils.URL_ROOT
          + "admin/index.jsp");

      selenium.findElement(By.linkText("Add a team")).click();

      selenium.findElement(By.name("teamNumber")).sendKeys(String.valueOf(teamNumber));
      selenium.findElement(By.name("teamName")).sendKeys(teamName);
      selenium.findElement(By.name("organization")).sendKeys(organization);

      selenium.findElement(By.id("tournament_"
          + tournament.getTournamentID())).click();

      final WebElement eventDivision = selenium.findElement(By.id("event_division_"
          + tournament.getTournamentID()));
      final Select eventDivisionSel = new Select(eventDivision);
      eventDivisionSel.selectByValue(division);

      final WebElement judgingStation = selenium.findElement(By.id("judging_station_"
          + tournament.getTournamentID()));
      final Select judgingStationSel = new Select(judgingStation);
      judgingStationSel.selectByValue(division);

      selenium.findElement(By.name("commit")).click();

      selenium.findElement(By.id("success"));

    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }

  }

  /**
   * Set the current tournament by name.
   * 
   * @param tournamentName the name of the tournament to make the current
   *          tournament
   * @throws IOException
   */
  public static void setTournament(final WebDriver selenium,
                                   final String tournamentName)
      throws IOException {
    try {
      loadPage(selenium, TestUtils.URL_ROOT
          + "admin/index.jsp");

      final WebElement currentTournament = selenium.findElement(By.id("currentTournamentSelect"));

      final Select currentTournamentSel = new Select(currentTournament);
      String tournamentID = null;
      for (final WebElement option : currentTournamentSel.getOptions()) {
        final String text = option.getText();
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("setTournament option: "
              + text);
        }
        if (text.endsWith("[ "
            + tournamentName + " ]")) {
          tournamentID = option.getAttribute("value");
        }
      }
      Assert.assertNotNull("Could not find tournament with name: "
          + tournamentName, tournamentID);

      currentTournamentSel.selectByValue(tournamentID);

      final WebElement changeTournament = selenium.findElement(By.name("change_tournament"));
      changeTournament.click();

      Assert.assertNotNull(selenium.findElement(By.id("success")));
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  /**
   * Create a web driver and set appropriate timeouts on it.
   */
  public static WebDriver createWebDriver() {
    final WebDriver selenium = new FirefoxDriver();
    selenium.manage().timeouts().implicitlyWait(500, TimeUnit.MILLISECONDS);
    selenium.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
    return selenium;
  }

  public static void initializePlayoffsForAwardGroup(final WebDriver selenium,
                                                     final String awardGroup)
      throws IOException {
    initializePlayoffsForAwardGroup(selenium, awardGroup, BracketSortType.SEEDING);
  }

  public static void initializePlayoffsForAwardGroup(final WebDriver selenium,
                                                     final String awardGroup,
                                                     final BracketSortType bracketSort)
      throws IOException {
    loadPage(selenium, TestUtils.URL_ROOT
        + "playoff");

    selenium.findElement(By.linkText("Create playoff bracket")).click();

    selenium.findElement(By.xpath("//input[@value='Create Playoff Bracket for Award Group "
        + awardGroup + "']")).click();
    Assert.assertTrue("Error creating bracket for award group: "
        + awardGroup, isElementPresent(selenium, By.id("success")));

    final Select initDiv = new Select(selenium.findElement(By.id("initialize-division")));
    initDiv.selectByValue(awardGroup);
    selenium.findElement(By.id("initialize_brackets")).click();
    Assert.assertFalse("Error loading page", isElementPresent(selenium, By.id("exception-handler")));

    final Select sort = new Select(selenium.findElement(By.id("sort")));
    sort.selectByValue(bracketSort.name());
    selenium.findElement(By.id("submit")).click();
    Assert.assertFalse("Error loading page", isElementPresent(selenium, By.id("exception-handler")));
  }

  /**
   * Try harder to find elements.
   */
  public static WebElement findElement(final WebDriver selenium,
                                       final By by,
                                       final int maxAttempts) {
    int attempts = 0;
    WebElement e = null;
    while (e == null
        && attempts <= maxAttempts) {
      try {
        e = selenium.findElement(by);
      } catch (final NoSuchElementException ex) {
        ++attempts;
        e = null;
        if (attempts >= maxAttempts) {
          throw ex;
        } else {
          LOGGER.warn("Trouble finding element, trying again", ex);
        }
      }
    }

    return e;
  }

  /**
   * Change the number of seeding rounds for the specified tournament.
   * 
   * @param selenium
   * @param newValue
   * @throws NoSuchElementException if there was a problem changing the value
   * @throws IOException if there is an error talking to selenium
   */
  public static void changeNumSeedingRounds(final WebDriver selenium,
                                            final int tournamentId,
                                            final int newValue)
      throws NoSuchElementException, IOException {
    try {
      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "admin/edit_all_parameters.jsp");
      final Select seedingRoundsSelection = new Select(selenium.findElement(By.name("seeding_rounds_"
          + tournamentId)));
      seedingRoundsSelection.selectByValue(Integer.toString(newValue));
      selenium.findElement(By.id("submit")).click();

      selenium.findElement(By.id("success"));
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  /**
   * Get the id of the current tournament
   * 
   * @throws IOException
   */
  public static int getCurrentTournamentId(final WebDriver selenium) throws IOException {
    try {
      loadPage(selenium, TestUtils.URL_ROOT
          + "admin/index.jsp");

      final WebElement currentTournament = selenium.findElement(By.id("currentTournamentSelect"));

      final Select currentTournamentSel = new Select(currentTournament);
      for (final WebElement option : currentTournamentSel.getOptions()) {
        if (option.isSelected()) {
          final String idStr = option.getAttribute("value");
          return Integer.valueOf(idStr);
        }
      }
      throw new FLLInternalException("Cannot find default tournament");
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  /**
   * Download the specified file and check the content type.
   * If the content type doesn't match an assertion violation will be thrown.
   * 
   * @param urlToLoad the page to load
   * @param destination where to save the file, may be null to not save the file
   *          and just check the content type
   */
  public static void downloadFile(final URL urlToLoad,
                                  final String expectedContentType,
                                  final Path destination)
      throws ClientProtocolException, IOException {

    try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
      final BasicHttpContext localContext = new BasicHttpContext();

      // if (this.mimicWebDriverCookieState) {
      // localContext.setAttribute(ClientContext.COOKIE_STORE,
      // mimicCookieState(selenium.manage().getCookies()));
      // }
      final HttpRequestBase requestMethod = new HttpGet();
      requestMethod.setURI(urlToLoad.toURI());
      // HttpParams httpRequestParameters = requestMethod.getParams();
      // httpRequestParameters.setParameter(ClientPNames.HANDLE_REDIRECTS,
      // this.followRedirects);
      // requestMethod.setParams(httpRequestParameters);

      final HttpResponse response = client.execute(requestMethod, localContext);

      final Header contentTypeHeader = response.getFirstHeader("Content-type");
      Assert.assertNotNull("Null content type header: "
          + urlToLoad.toString(), contentTypeHeader);
      final String contentType = contentTypeHeader.getValue().split(";")[0].trim();
      Assert.assertEquals("Unexpected content type from: "
          + urlToLoad.toString(), expectedContentType, contentType);

      if (null != destination) {
        try (final InputStream stream = response.getEntity().getContent()) {
          Files.copy(stream, destination);
        } // try create stream
      } // non-null destination

    } catch (final URISyntaxException e) {
      throw new FLLInternalException("Got exception turning URL into URI, this shouldn't happen", e);
    }
  }

}

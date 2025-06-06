/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.CookieManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.catalina.LifecycleException;
import org.apache.http.client.ClientProtocolException;
import org.fest.swing.image.ScreenshotTaker;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.lessThan;

import fll.Launcher;
import fll.TestUtils;
import fll.Tournament;
import fll.Utilities;
import fll.db.RunMetadata;
import fll.tomcat.TomcatLauncher;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.api.TournamentsServlet;
import fll.xml.BracketSortType;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Some utilities for integration tests.
 */
public final class IntegrationTestUtils {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Username used for integration tests.
   */
  public static final String TEST_USERNAME = "fll_test";

  /**
   * Password used for integration tests.
   */
  public static final String TEST_PASSWORD = "Lego";

  /**
   * Maximum amount of time to wait for an element to appear.
   *
   * @see #createWebDriverWait(WebDriver)
   */
  private static final Duration WAIT_FOR_ELEMENT = Duration.ofSeconds(10);

  /**
   * How long to wait between polls of the page for a web element with the waiter.
   *
   * @see #createWebDriverWait(WebDriver)
   */
  private static final Duration WAIT_FOR_ELEMENT_POLL_INTERVAL = Duration.ofMillis(100);

  private IntegrationTestUtils() {
    // no instances
  }

  /**
   * Check if an element exists.
   * 
   * @param selenium web browser driver
   * @param search what to search for
   * @return if the element is found
   */
  public static boolean isElementPresent(final WebDriver selenium,
                                         final By search) {
    boolean elementFound = false;
    try {
      selenium.findElement(search);
      elementFound = true;
    } catch (final NoSuchElementException e) {
      elementFound = false;
    }
    return elementFound;
  }

  /**
   * Calls {@link #loadPage(WebDriver, WebDriverWait, String, ExpectedCondition)}
   * with a condition checking that the current url contains the specified url.
   *
   * @param selenium web driver
   * @param seleniumWait wait for elements
   * @param url the url to load an check
   */
  public static void loadPage(final WebDriver selenium,
                              final WebDriverWait seleniumWait,
                              final String url) {
    loadPage(selenium, seleniumWait, url, ExpectedConditions.urlContains(url));
  }

  /**
   * Load a page and check to make sure the page didn't crash.
   *
   * @param selenium the test controller
   * @param seleniumWait wait for elements
   * @param url the page to load
   * @param pageLoaded condition to know when the page has loaded
   */
  public static void loadPage(final WebDriver selenium,
                              final WebDriverWait seleniumWait,
                              final String url,
                              final ExpectedCondition<Boolean> pageLoaded) {
    selenium.get(url);

    seleniumWait.until(pageLoaded);

    assertNoException(selenium);
  }

  /**
   * Assert that the current page is not the error handler page.
   * 
   * @param selenium web browser driver
   */
  public static void assertNoException(final WebDriver selenium) {
    assertFalse(isElementPresent(selenium, By.id("exception-handler")), "Error loading page");
  }

  /**
   * Initialize the database using the given challenge document.
   *
   * @param driver the test controller
   * @param driverWait wait for elements
   * @param challengeDocument the challenge descriptor
   * @throws IOException if there is an error talking to the server
   */
  public static void initializeDatabase(final WebDriver driver,
                                        final WebDriverWait driverWait,
                                        final Document challengeDocument)
      throws IOException {
    assertNotNull(challengeDocument);

    final Path challengeFile = Files.createTempFile("fll", ".xml");
    try (Writer writer = Files.newBufferedWriter(challengeFile, Utilities.DEFAULT_CHARSET)) {
      XMLUtils.writeXML(challengeDocument, writer);
    }
    try {
      initializeDatabase(driver, driverWait, challengeFile);
    } finally {
      Files.delete(challengeFile);
    }
  }

  /**
   * Initialize the database using the given challenge descriptor.
   *
   * @param driver the test controller
   * @param driverWait wait for elements
   * @param challengeStream the challenge descriptor
   * @throws IOException if there is an error talking to the server
   */
  public static void initializeDatabase(final WebDriver driver,
                                        final WebDriverWait driverWait,
                                        final InputStream challengeStream)
      throws IOException {
    assertNotNull(challengeStream);

    final Path challengeFile = Files.createTempFile("fll", ".xml");
    Files.copy(challengeStream, challengeFile, StandardCopyOption.REPLACE_EXISTING);
    try {
      initializeDatabase(driver, driverWait, challengeFile);
    } finally {
      Files.delete(challengeFile);
    }
  }

  /**
   * Initialize the database using the given challenge descriptor.
   *
   * @param driver the test controller
   * @param driverWait used to wait for elements
   * @param challengeFile a file to read the challenge description from. This
   *          file will not be deleted.
   */
  public static void initializeDatabase(final WebDriver driver,
                                        final WebDriverWait driverWait,
                                        final Path challengeFile) {

    LOGGER.trace("Visiting setup");
    driver.get(TestUtils.URL_ROOT
        + "setup/");

    // wait for the login page or the setup page to load
    driverWait.until(ExpectedConditions.or(ExpectedConditions.presenceOfElementLocated(By.name("submit_login")),
                                           ExpectedConditions.urlContains("/setup")));

    if (isElementPresent(driver, By.name("submit_login"))) {
      LOGGER.trace("Login required");
      login(driver);

      LOGGER.trace("Visiting setup after login");
      driver.get(TestUtils.URL_ROOT
          + "setup/");
    }

    final WebElement fileEle = driverWait.until(ExpectedConditions.elementToBeClickable(By.name("xmldocument")));
    fileEle.sendKeys(challengeFile.toAbsolutePath().toString());

    final boolean expectAlert = driver.getPageSource()
                                      .contains("This will erase ALL scores and team information in the database");

    final WebElement reinitDB = driver.findElement(By.name("reinitializeDatabase"));
    reinitDB.click();
    LOGGER.trace("Clicked reinitializeDatabase");

    finishDatabaseInitialization(driver, driverWait, expectAlert);
  }

  /**
   * Initialize a database from a zip file.
   *
   * @param selenium the test controller
   * @param seleniumWait wait for elements
   * @param inputStream input stream that has database to load in it, this input
   *          stream is closed by this method upon successful completion
   * @throws IOException if there is an error talking to the server
   */
  public static void initializeDatabaseFromDump(final WebDriver selenium,
                                                final WebDriverWait seleniumWait,
                                                final InputStream inputStream)
      throws IOException {
    assertNotNull(inputStream);

    final File dumpFile = IntegrationTestUtils.storeInputStreamToFile(inputStream);
    try {
      selenium.get(TestUtils.URL_ROOT
          + "setup/");

      // wait for the login page or the setup page to load
      seleniumWait.until(ExpectedConditions.or(ExpectedConditions.presenceOfElementLocated(By.name("submit_login")),
                                               ExpectedConditions.urlContains("/setup")));

      if (isElementPresent(selenium, By.name("submit_login"))) {
        login(selenium);

        selenium.get(TestUtils.URL_ROOT
            + "setup/");
      }

      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.name("dbdump")))
                  .sendKeys(dumpFile.getAbsolutePath());

      final boolean expectAlert = selenium.getPageSource()
                                          .contains("This will erase ALL scores and team information in the database");

      final WebElement createEle = selenium.findElement(By.name("createdb"));
      createEle.click();
      LOGGER.trace("Clicked createdb button");

      finishDatabaseInitialization(selenium, seleniumWait, expectAlert);
    } finally {
      if (!dumpFile.delete()) {
        dumpFile.deleteOnExit();
      }
    }
    login(selenium);
  }

  private static void finishDatabaseInitialization(final WebDriver selenium,
                                                   final WebDriverWait seleniumWait,
                                                   final boolean expectAlert) {
    if (expectAlert) {
      handleDatabaseEraseConfirmation(selenium, seleniumWait);
    }

    // wait for all possible conditions that can me the server has finished creating
    // the database
    seleniumWait.until(ExpectedConditions.or(ExpectedConditions.presenceOfElementLocated(By.id("success")),
                                             ExpectedConditions.presenceOfElementLocated(By.id("exception-handler")),
                                             ExpectedConditions.urlContains("import-users.jsp"),
                                             ExpectedConditions.urlContains("ask-create-admin.jsp")));

    assertNoException(selenium);

    handleImportUsers(selenium);

    LOGGER.trace("Found database success, calling createUser");
    createUser(selenium, seleniumWait);

    LOGGER.trace("Finished with create user, calling login");
    login(selenium);
  }

  private static void handleImportUsers(final WebDriver driver) {
    if (isElementPresent(driver, By.name("import_users"))) {
      // uncheck all usernames

      final WebElement parent = driver.findElement(By.id("import-users-form"));

      final List<WebElement> children = parent.findElements(By.cssSelector("input:checked[type='checkbox']"));
      for (final WebElement input : children) {
        input.click();
      }

      driver.findElement(By.name("import_users")).click();
    }
  }

  private static void createUser(final WebDriver selenium,
                                 final WebDriverWait seleniumWait) {

    seleniumWait.until(ExpectedConditions.or(ExpectedConditions.urlContains("createUsername.jsp"),
                                             ExpectedConditions.presenceOfElementLocated(By.name("submit_create_admin"))));

    if (isElementPresent(selenium, By.name("submit_create_admin"))) {
      // create new admin user
      selenium.findElement(By.name("submit_create_admin")).click();
    }

    seleniumWait.until(ExpectedConditions.urlContains("createUsername.jsp"));

    final WebElement userElement = seleniumWait.until(ExpectedConditions.elementToBeClickable(By.name("user")));
    userElement.sendKeys(TEST_USERNAME);

    final WebElement passElement = selenium.findElement(By.name("pass"));
    passElement.sendKeys(TEST_PASSWORD);

    final WebElement passCheckElement = selenium.findElement(By.name("pass_check"));
    passCheckElement.sendKeys(TEST_PASSWORD);

    final WebElement submitElement = selenium.findElement(By.name("submit_create_user"));
    submitElement.click();
    LOGGER.trace("Submitted create user page");

    // should work, but something on the CI system is loading /setup in here as well
    // seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id("success-create-user")));

    seleniumWait.until(ExpectedConditions.not(ExpectedConditions.urlContains("createUsername.jsp")));
  }

  private static void handleDatabaseEraseConfirmation(final WebDriver selenium,
                                                      final WebDriverWait seleniumWait) {
    seleniumWait.until(ExpectedConditions.alertIsPresent());
    final Alert confirmCreateDB = selenium.switchTo().alert();
    LOGGER.info("Confirmation text: "
        + confirmCreateDB.getText());
    confirmCreateDB.accept();
  }

  /**
   * Defaults filePrefix to "fll".
   *
   * @param driver used to get the screen shot
   * @see #storeScreenshot(String, WebDriver)
   * @throws IOException if there is an error getting the screen shot
   */
  public static void storeScreenshot(final WebDriver driver) throws IOException {
    storeScreenshot("fll", driver);
  }

  /**
   * Make sure that the directory used to store screen shots exists.
   * 
   * @return the directory to store screen shots in
   * @throws IOException if there is an error creating the directory
   */
  public static Path ensureScreenshotDirectoryExists() throws IOException {
    final Path screenshotsDir = Paths.get("screenshots");
    if (!Files.exists(screenshotsDir)) {
      Files.createDirectories(screenshotsDir);
    }
    return screenshotsDir;
  }

  /**
   * Store screenshot and other information for debugging the error.
   *
   * @param filePrefix prefix for the files that are created
   * @param driver used to get the screen shot
   * @throws IOException if there is an error getting the screen shot
   */
  public static void storeScreenshot(final String filePrefix,
                                     final WebDriver driver)
      throws IOException {
    final Path screenshotsDir = ensureScreenshotDirectoryExists();

    final Path tempDir = Files.createTempDirectory(screenshotsDir, filePrefix);

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
    try (BufferedWriter writer = Files.newBufferedWriter(htmlFile)) {
      writer.write(html);
    }
    LOGGER.info("HTML saved to "
        + htmlFile.toAbsolutePath().toString());

    try {
      // get the database
      final Path dbOutput = tempDir.resolve("database.flldb");
      LOGGER.info("Downloading database to "
          + dbOutput.toAbsolutePath());
      downloadFile(new URI(TestUtils.URL_ROOT
          + "admin/database.flldb"), null, dbOutput);
    } catch (final URISyntaxException e) {
      LOGGER.error("Error creating address to download database for screen shot", e);
    } catch (final InterruptedException e) {
      LOGGER.error("Interrupted downloading database for screen shot", e);
    }
  }

  /**
   * Copy the contents of a stream to a temporary file.
   *
   * @param inputStream the data to store in the temporary file
   * @return the temporary file, you need to delete it
   * @throws IOException if there is an error talking to the server
   */
  public static File storeInputStreamToFile(final InputStream inputStream) throws IOException {
    final File tempFile = File.createTempFile("fll", null);
    try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
      final byte[] buffer = new byte[1042];
      int bytesRead;
      while (-1 != (bytesRead = inputStream.read(buffer))) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }

    return tempFile;
  }

  /**
   * Login to software.
   *
   * @param driver browser driver
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
    final StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  private static String readJSON(final String url) throws IOException, URISyntaxException {
    final InputStream is = new URI(url).toURL().openStream();
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
   * @param tournamentName name of tournament
   * @return the tournament or null if not found
   * @throws IOException if there is an error talking to the server
   */
  public static Tournament getTournamentByName(final String tournamentName) throws IOException {
    try {
      final String json = readJSON(TestUtils.URL_ROOT
          + "api/Tournaments");

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Tournaments json: "
            + json);
      }

      // get the JSON
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      final Reader reader = new StringReader(json);

      final Collection<Tournament> tournaments = jsonMapper.readValue(reader,
                                                                      TournamentsServlet.TournamentsTypeInformation.INSTANCE);

      for (final Tournament tournament : tournaments) {
        if (tournament.getName().equals(tournamentName)) {
          return tournament;
        }
      }

      return null;
    } catch (final URISyntaxException e) {
      throw new FLLInternalException("Error parsing URL", e);
    }
  }

  /**
   * Add a team to a tournament.
   *
   * @param seleniumWait used to wait for elements
   * @param selenium web browser driver
   * @param teamNumber team number
   * @param teamName team name
   * @param organization organization
   * @param division award group
   * @param tournamentName tournament
   * @throws IOException if there is an error talking to the server
   */
  public static void addTeam(final WebDriver selenium,
                             final WebDriverWait seleniumWait,
                             final int teamNumber,
                             final String teamName,
                             final String organization,
                             final String division,
                             final String tournamentName)
      throws IOException {
    final Tournament tournament = getTournamentByName(tournamentName);

    loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
        + "admin/index.jsp");

    selenium.findElement(By.linkText("Add a team")).click();

    seleniumWait.until(ExpectedConditions.elementToBeClickable(By.name("teamNumber")))
                .sendKeys(String.valueOf(teamNumber));
    selenium.findElement(By.name("teamName")).sendKeys(teamName);
    selenium.findElement(By.name("organization")).sendKeys(organization);

    selenium.findElement(By.id("tournament_"
        + tournament.getTournamentID())).click();

    final WebElement eventDivision = seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("event_division_"
        + tournament.getTournamentID())));
    final Select eventDivisionSel = new Select(eventDivision);
    eventDivisionSel.selectByValue(division);

    final WebElement judgingStation = selenium.findElement(By.id("judging_station_"
        + tournament.getTournamentID()));
    final Select judgingStationSel = new Select(judgingStation);
    judgingStationSel.selectByValue(division);

    selenium.findElement(By.name("commit")).click();

    seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id("success")));
  }

  /**
   * Set the current tournament by name.
   *
   * @param seleniumWait wait for elements
   * @param selenium the browser driver
   * @param tournamentName the name of the tournament to make the current
   *          tournament
   * @throws IOException if there is an error talking to the server
   */
  public static void setTournament(final WebDriver selenium,
                                   final WebDriverWait seleniumWait,
                                   final String tournamentName)
      throws IOException {
    loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
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
          + tournamentName
          + " ]")) {
        tournamentID = option.getDomProperty("value");
      }
    }
    assertNotNull(tournamentID, "Could not find tournament with name: "
        + tournamentName);

    currentTournamentSel.selectByValue(tournamentID);

    final WebElement changeTournament = selenium.findElement(By.name("change_tournament"));
    changeTournament.click();

    assertNotNull(selenium.findElement(By.id("success")));
  }

  /**
   * Create firefox web driver used for most integration tests.
   *
   * @return {@link #createWebDriver(WebDriverType)}
   */
  public static WebDriver createWebDriver() {
    return createWebDriver(WebDriverType.FIREFOX);
  }

  /**
   * Types of web browser to create drivers for.
   */
  public enum WebDriverType {
    /** firefox web browser. */
    FIREFOX,
    /** chrome web browser. */
    CHROME
  }

  private static Set<WebDriverType> mInitializedWebDrivers = new HashSet<>();

  /**
   * Create a web driver and set appropriate timeouts on it.
   * 
   * @return web browser driver
   * @param type which browser to use
   */
  public static WebDriver createWebDriver(final WebDriverType type) {
    final WebDriver selenium;
    switch (type) {
    case FIREFOX:
      selenium = createFirefoxWebDriver();
      break;
    case CHROME:
      selenium = createChromeWebDriver();
      break;
    default:
      throw new IllegalArgumentException("Unknown web driver type: "
          + type);
    }

    // selenium.manage().timeouts().implicitlyWait(WAIT_FOR_PAGE_LOAD_MS,
    // TimeUnit.MILLISECONDS);
    // selenium.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);

    // get some information from the driver
    LOGGER.info("Selenium driver: "
        + selenium.getClass().getName());
    if (selenium instanceof JavascriptExecutor) {
      final JavascriptExecutor jsSelenium = (JavascriptExecutor) selenium;
      final String uAgent = jsSelenium.executeScript("return navigator.userAgent;").toString();
      LOGGER.info("User agent: "
          + uAgent);
    }

    return selenium;
  }

  private static WebDriver createFirefoxWebDriver() {
    if (!mInitializedWebDrivers.contains(WebDriverType.FIREFOX)) {
      WebDriverManager.getInstance(DriverManagerType.FIREFOX).setup();
      mInitializedWebDrivers.add(WebDriverType.FIREFOX);
    }

    // final DesiredCapabilities capabilities = DesiredCapabilities.firefox();
    // capabilities.setCapability("marionette", true);
    // final WebDriver selenium = new FirefoxDriver(capabilities);

    final FirefoxOptions options = new FirefoxOptions();
    // options.setLogLevel(org.openqa.selenium.firefox.FirefoxDriverLogLevel.TRACE);
    final WebDriver selenium = new FirefoxDriver(options);

    // final WebDriver selenium = new FirefoxDriver();
    return selenium;
  }

  private static WebDriver createChromeWebDriver() {
    if (!mInitializedWebDrivers.contains(WebDriverType.CHROME)) {
      WebDriverManager.getInstance(DriverManagerType.CHROME).setup();
      mInitializedWebDrivers.add(WebDriverType.CHROME);
    }

    final WebDriver selenium = new ChromeDriver();

    return selenium;
  }

  /**
   * Configure the server using the specified {@link RunMetadata}.
   * 
   * @param selenium test driver
   * @param seleniumWait test waiter
   * @param runMetadata metadata to configure on the server
   */
  public static void configureRunMetadata(final WebDriver selenium,
                                          final WebDriverWait seleniumWait,
                                          final RunMetadata runMetadata) {
    loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
        + "admin/edit_run_metadata.jsp");

    final String displayNameInputId = String.format("%d_name", runMetadata.getRunNumber());
    // ensure the run exists
    final WebElement addRowButton = selenium.findElement(By.id("addRow"));

    while (true) {
      try {
        seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id(displayNameInputId)));
        break;
      } catch (final TimeoutException e) {
        LOGGER.debug("Didn't find element {}, adding a row", displayNameInputId, e);
        addRowButton.click();
      }
    }

    // configure page
    final WebElement nameInput = selenium.findElement(By.id(displayNameInputId));
    nameInput.clear();
    nameInput.sendKeys(runMetadata.getDisplayName());

    final WebElement runTypeInput = selenium.findElement(By.id(String.format("%d_runType",
                                                                             runMetadata.getRunNumber())));
    final Select runTypeSelect = new Select(runTypeInput);
    runTypeSelect.selectByValue(runMetadata.getRunType().name());

    final WebElement scoreboardDisplayInput = selenium.findElement(By.id(String.format("%d_scoreboardDisplay",
                                                                                       runMetadata.getRunNumber())));
    if (runMetadata.isScoreboardDisplay()
        && !scoreboardDisplayInput.isSelected()) {
      scoreboardDisplayInput.click();
    } else if (!runMetadata.isScoreboardDisplay()
        && scoreboardDisplayInput.isSelected()) {
      scoreboardDisplayInput.click();
    }

    selenium.findElement(By.id("submit_data")).click();

    // wait for positive response
    seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id("success")));
  }

  /**
   * Uses {@link BracketSortType#SEEDING} as the sort.
   *
   * @param selenium passed along
   * @param seleniumWait passed along
   * @param awardGroup passed along
   * @param enableThirdPlace passed along
   * @see #initializePlayoffsForAwardGroup(WebDriver, WebDriverWait, String,
   *      BracketSortType, boolean)
   */
  public static void initializePlayoffsForAwardGroup(final WebDriver selenium,
                                                     final WebDriverWait seleniumWait,
                                                     final String awardGroup,
                                                     final boolean enableThirdPlace) {
    initializePlayoffsForAwardGroup(selenium, seleniumWait, awardGroup, BracketSortType.SEEDING, enableThirdPlace);
  }

  /**
   * Create and initialize playoffs for the specified award group.
   *
   * @param selenium web browser controller
   * @param seleniumWait wait for elements
   * @param awardGroup the award group
   * @param bracketSort how to sort teams
   * @param enableThirdPlace true if 3rd place bracket should be enabled
   */
  public static void initializePlayoffsForAwardGroup(final WebDriver selenium,
                                                     final WebDriverWait seleniumWait,
                                                     final String awardGroup,
                                                     final BracketSortType bracketSort,
                                                     final boolean enableThirdPlace) {
    loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
        + "playoff");

    selenium.findElement(By.id("create-bracket")).click();

    seleniumWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Create Head to Head Bracket for Award Group "
        + awardGroup
        + "']"))).click();
    seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id("success")));

    if (BracketSortType.CUSTOM.equals(bracketSort)) {
      throw new FLLRuntimeException("Custom bracket sort type isn't supported");
    }

    initializePlayoffBracketCommon(selenium, seleniumWait, awardGroup, Collections.emptyList(), bracketSort,
                                   enableThirdPlace);
  }

  /**
   * Uses {@link BracketSortType#SEEDING} as the sort.
   *
   * @param selenium passed along
   * @param seleniumWait passed along
   * @param bracketName passed along
   * @param teamNumbers passed along
   * @param enableThirdPlace passed along
   * @see #initializePlayoffBracket(WebDriver, WebDriverWait, String, List,
   *      BracketSortType, boolean)
   */
  public static void initializePlayoffBracket(final WebDriver selenium,
                                              final WebDriverWait seleniumWait,
                                              final String bracketName,
                                              final List<Integer> teamNumbers,
                                              final boolean enableThirdPlace) {
    initializePlayoffBracket(selenium, seleniumWait, bracketName, teamNumbers, BracketSortType.SEEDING,
                             enableThirdPlace);
  }

  /**
   * Create and initialize a bracket based on the name and specified team numbers.
   * 
   * @param selenium web browser driver
   * @param seleniumWait wait for elements
   * @param bracketName name of the bracket to create
   * @param teamNumbers teams to put in the bracket
   * @param bracketSort how to sort teams
   * @param enableThirdPlace true if 3rd place bracket should be enabled
   */
  public static void initializePlayoffBracket(final WebDriver selenium,
                                              final WebDriverWait seleniumWait,
                                              final String bracketName,
                                              final List<Integer> teamNumbers,
                                              final BracketSortType bracketSort,
                                              final boolean enableThirdPlace) {
    loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
        + "playoff");
    selenium.findElement(By.id("create-bracket")).click();

    seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.name("bracket_name"))).sendKeys(bracketName);

    // select the teams
    for (final Integer teamNumber : teamNumbers) {
      final String elementId = String.format("select_%d", teamNumber);
      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id(elementId))).click();
    }

    selenium.findElement(By.name("selected_teams")).click();

    seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id("success")));

    initializePlayoffBracketCommon(selenium, seleniumWait, bracketName, teamNumbers, bracketSort, enableThirdPlace);
  }

  private static void initializePlayoffBracketCommon(final WebDriver selenium,
                                                     final WebDriverWait seleniumWait,
                                                     final String bracketName,
                                                     final List<Integer> teamNumbers,
                                                     final BracketSortType bracketSort,
                                                     final boolean enableThirdPlace) {
    final Select initDiv = new Select(selenium.findElement(By.id("initialize-division")));
    initDiv.selectByValue(bracketName);

    if (enableThirdPlace) {
      selenium.findElement(By.name("enableThird")).click();
    }

    selenium.findElement(By.id("initialize_brackets")).click();

    final WebElement sortElement = seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("sort")));
    final Select sort = new Select(sortElement);
    sort.selectByValue(bracketSort.name());

    if (BracketSortType.CUSTOM.equals(bracketSort)) {
      final String teamsStr = teamNumbers.stream().map(String::valueOf).collect(Collectors.joining(","));
      selenium.findElement(By.id("custom_order")).sendKeys(teamsStr);
    }

    selenium.findElement(By.id("submit_data")).click();

    seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id("success")));
  }

  /**
   * Get the id of the current tournament.
   *
   * @param seleniumWait wait for elements
   * @param selenium browser driver
   * @throws IOException test error
   * @return id of current tournament
   */
  public static int getCurrentTournamentId(final WebDriver selenium,
                                           final WebDriverWait seleniumWait)
      throws IOException {
    loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
        + "admin/index.jsp");

    final WebElement currentTournament = selenium.findElement(By.id("currentTournamentSelect"));

    final Select currentTournamentSel = new Select(currentTournament);
    for (final WebElement option : currentTournamentSel.getOptions()) {
      if (option.isSelected()) {
        final String idStr = option.getDomProperty("value");
        return Integer.parseInt(idStr);
      }
    }
    throw new FLLInternalException("Cannot find default tournament");
  }

  /**
   * HTTP codes with this value and greater are errors.
   */
  private static final int HTTP_ERROR_BASE = 400;

  /**
   * Login to the software. This depends on {@code client} having a cookie handler
   * to track the session cookie.
   * This is used to login without selenium.
   * 
   * @param client the client to make the request with
   * @throws InterruptedException if the client is interrupted posting to the
   *           login page
   * @throws IOException if there is an error posting to the login page
   */
  private static void login(final HttpClient client) throws IOException, InterruptedException {
    final Map<String, String> loginData = new HashMap<>();
    loginData.put("user", TEST_USERNAME);
    loginData.put("pass", TEST_PASSWORD);

    final String formData = loginData.entrySet().stream().map(entry -> entry.getKey()
        + "="
        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));

    try {
      final HttpRequest loginRequest = HttpRequest.newBuilder()//
                                                  .uri(new URI(TestUtils.URL_ROOT
                                                      + "DoLogin")) //
                                                  .header("Content-Type", "application/x-www-form-urlencoded")
                                                  .POST(BodyPublishers.ofString(formData, StandardCharsets.UTF_8))
                                                  .build();
      final HttpResponse<String> loginResponse = client.send(loginRequest, BodyHandlers.ofString());
      assertThat("Error logging in", loginResponse.statusCode(), lessThan(HTTP_ERROR_BASE));
    } catch (final URISyntaxException e) {
      LOGGER.error("Error creating login address", e);
      fail("Internal error, login address does not convert to a URI: "
          + e.getMessage());
    }

  }

  /**
   * Download the specified file and check the content type.
   * If the content type doesn't match an assertion violation will be thrown.
   *
   * @param urlToLoad the page to load
   * @param expectedContentType the expected content type.
   *          If the expected type is null, skip this check.
   * @param destination where to save the file. If null don't save the file.
   *          Any existing file will be
   *          overwritten.
   * @throws ClientProtocolException if there is an error talking to the server
   * @throws IOException if there is an error talking to the server
   * @throws InterruptedException if the request is interrupted
   */
  public static void downloadFile(final URI urlToLoad,
                                  final String expectedContentType,
                                  final Path destination)
      throws ClientProtocolException, IOException, InterruptedException {

    final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();

    login(client);

    final HttpRequest downloadRequest = HttpRequest.newBuilder().uri(urlToLoad).build();

    final HttpResponse<InputStream> downloadResponse = client.send(downloadRequest, BodyHandlers.ofInputStream());
    assertThat("Error downloading content from "
        + urlToLoad, downloadResponse.statusCode(), lessThan(HTTP_ERROR_BASE));

    if (null != expectedContentType) {
      final Optional<String> contentTypeHeader = downloadResponse.headers().firstValue("Content-type");
      assertTrue(contentTypeHeader.isPresent(), "Null content type header: "
          + urlToLoad.toString());

      final String contentType = contentTypeHeader.get().split(";")[0].trim();
      assertEquals(expectedContentType, contentType, "Unexpected content type from: "
          + urlToLoad.toString());
    }

    if (null != destination) {
      try (InputStream stream = downloadResponse.body()) {
        Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
      } // try create stream
    } // non-null destination
  }

  /**
   * Used to allocate tomcat around a test.
   */
  public static class TomcatRequired
      implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    private static final String TOMCAT_LAUNCHER_KEY = "TomcatLauncher";

    private static final String WEBDRIVER_KEY = "WebDriver";

    private static final String WAIT_KEY = "Wait";

    private Store getStore(final ExtensionContext context) {
      return context.getStore(Namespace.create(getClass(), context.getRequiredTestMethod()));
    }

    @Override
    public void beforeTestExecution(final ExtensionContext context) throws Exception {
      // remove the database first to ensure that we are always at the same starting
      // state
      removeDatabase();

      final TomcatLauncher launcher = new TomcatLauncher(Launcher.DEFAULT_WEB_PORT);
      try {
        LOGGER.info("Starting tomcat");
        launcher.start();
      } catch (final LifecycleException e) {
        LOGGER.error("Error starting tomcat", e);
        throw new RuntimeException(e);
      }
      getStore(context).put(TOMCAT_LAUNCHER_KEY, launcher);

      LOGGER.info("Starting selenium");
      final WebDriver selenium = createWebDriver();
      getStore(context).put(WEBDRIVER_KEY, selenium);

      final WebDriverWait wait = createWebDriverWait(selenium);
      getStore(context).put(WAIT_KEY, wait);
    }

    @Override
    public void afterTestExecution(final ExtensionContext context) throws Exception {
      final WebDriver selenium = getStore(context).remove(WEBDRIVER_KEY, WebDriver.class);
      if (null != selenium) {
        LOGGER.info("Shutting down selenium");
        selenium.quit();
      } else {
        LOGGER.error("Selenium doesn't exist, cannot be shutdown");
      }

      final TomcatLauncher launcher = getStore(context).remove(TOMCAT_LAUNCHER_KEY, TomcatLauncher.class);
      try {
        if (null != launcher) {
          LOGGER.info("Stopping tomcat");
          launcher.stop();
        } else {
          LOGGER.error("Tomcat doesn't exist, cannot be shutdown");
        }
      } catch (final LifecycleException e) {
        LOGGER.error("Error shutting down tomcat", e);
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext,
                                     final ExtensionContext extensionContext)
        throws ParameterResolutionException {
      final Class<?> type = parameterContext.getParameter().getType();
      return WebDriver.class.isAssignableFrom(type)
          || WebDriverWait.class.isAssignableFrom(type);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext,
                                   final ExtensionContext extensionContext)
        throws ParameterResolutionException {
      final Class<?> type = parameterContext.getParameter().getType();
      if (WebDriver.class.isAssignableFrom(type)) {
        final WebDriver selenium = getStore(extensionContext).get(WEBDRIVER_KEY, WebDriver.class);
        if (null != selenium) {
          return selenium;
        } else {
          throw new ParameterResolutionException("The webdriver has not been created. This is an internal error");
        }
      } else if (WebDriverWait.class.isAssignableFrom(type)) {
        final WebDriverWait wait = getStore(extensionContext).get(WAIT_KEY, WebDriverWait.class);
        if (null != wait) {
          return wait;
        } else {
          throw new ParameterResolutionException("The webdriver wait has not been created. This is an internal error");
        }
      } else {
        throw new ParameterResolutionException("Unknown parameter type: "
            + type.getName());
      }
    }

    private static void removeDatabase() throws IOException {
      final Path classesPath = TomcatLauncher.getClassesPath();
      LOGGER.debug("Classes path {}", classesPath);

      final Path webappRoot = TomcatLauncher.findWebappRoot(classesPath);
      if (null == webappRoot) {
        throw new FLLRuntimeException("Cannot find location of the database");
      }
      LOGGER.debug("Web app root: {}", webappRoot);
      final Path webInfDir = webappRoot.resolve("WEB-INF");
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(webInfDir)) {
        for (final Path path : stream) {
          final Path fname = path.getFileName();
          if (null != fname
              && fname.toString().startsWith("flldb.")) {
            LOGGER.debug("Deleting datbase file: {}", fname);
            Files.delete(path);
          } else {
            LOGGER.debug("Not deleting WEB-INF file: {} path: {}", fname, path);
          }
        }
      }
    }

  }

  /**
   * Used to take screen shots.
   */
  private static final ScreenshotTaker SCREENSHOT_TAKER = new ScreenshotTaker();

  /**
   * Save a screen shot. Used for UI tests.
   *
   * @throws IOException if there is an error saving the file
   */
  public static void saveScreenshot() throws IOException {
    final File screenshotDir = new File("screenshots");
    if (!screenshotDir.exists()) {
      if (!screenshotDir.mkdirs()) {
        throw new RuntimeException("Cannot make directories "
            + screenshotDir);
      }
    }

    final File screenshot = File.createTempFile("fll", ".png", screenshotDir);
    LOGGER.error("Screenshot saved to "
        + screenshot.getAbsolutePath());
    // file can't exist when calling save desktop as png
    if (screenshot.exists()
        && !screenshot.delete()) {
      throw new RuntimeException("Cannot delete screenshot file "
          + screenshot);
    }
    SCREENSHOT_TAKER.saveDesktopAsPng(screenshot.getAbsolutePath());
  }

  /**
   * Set the running head to head tournament parameter. Make sure that the current
   * tournament is set before calling this method.
   *
   * @param selenium the web driver
   * @param seleniumWait wait for elements
   * @param runningHeadToHead the value of running head to head
   * @throws IOException see {@link #loadPage(WebDriver, WebDriverWait, String)}
   */
  public static void setRunningHeadToHead(final WebDriver selenium,
                                          final WebDriverWait seleniumWait,
                                          final boolean runningHeadToHead)
      throws IOException {
    loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
        + "admin/edit_tournament_parameters.jsp");

    final WebElement element = seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("running_head_to_head")));
    if (runningHeadToHead != element.isSelected()) {
      element.click();
    }

    selenium.findElement(By.id("submit_data")).click();

    assertNotNull(selenium.findElement(By.id("success")));
  }

  /**
   * Click the submit score button and the confirmation dialog.
   * An assertion violation occurs if the submit button is not enabled.
   *
   * @param selenium the web driver
   * @param seleniumWait wait for elements
   */
  public static void submitPerformanceScore(final WebDriver selenium,
                                            final WebDriverWait seleniumWait) {
    // check that the submit button is active
    assertTrue(selenium.findElement(By.id("submit_score")).isEnabled(),
               "Submit button is not enabled, invalid score entered");

    selenium.findElement(By.id("submit_score")).click();

    // wait for yes/no dialog
    final WebElement confirmScoreYesButton = seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("yesno-dialog_yes")));
    confirmScoreYesButton.click();

    // wait for upload dialog
    final WebElement uploadCloseButton = seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("score-entry-upload_close")));
    uploadCloseButton.click();
  }

  /**
   * Create a waiter for the specified driver.
   *
   * @param selenium web browser driver
   * @return waiter for elements
   */
  public static WebDriverWait createWebDriverWait(final WebDriver selenium) {
    return new WebDriverWait(selenium, WAIT_FOR_ELEMENT, WAIT_FOR_ELEMENT_POLL_INTERVAL);
  }

}

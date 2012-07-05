/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.thoughtworks.selenium.Selenium;

import fll.TestUtils;
import fll.util.LogUtils;

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
   * Load a page and check to make sure the page didn't crash.
   * 
   * @param selenium the test controller
   * @param url the page to load
   * @throws IOException
   */
  public static void loadPage(final Selenium selenium,
                              final String url) throws IOException {
    try {
      selenium.open(url);
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      final boolean error = selenium.isTextPresent("Exception");
      Assert.assertFalse("Error loading: "
          + url, error);
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  /**
   * Check if an element exists.
   */
  public static boolean elementExists(final WebDriver selenium,
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
                              final String url) throws IOException {
    try {
      selenium.get(url);

      Assert.assertFalse("Error loading page", elementExists(selenium, By.id("exception-handler")));
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

  /**
   * Initialize the database using the given challenge descriptor.
   * 
   * @param driver the test controller
   * @param challengeStream the challenge descriptor
   * @param forceRebuild if true, then force the database to be rebuilt
   * @throws IOException
   */
  public static void initializeDatabase(final WebDriver driver,
                                        final InputStream challengeStream,
                                        final boolean forceRebuild) throws IOException {
    try {
      Assert.assertNotNull(challengeStream);
      final File challengeFile = IntegrationTestUtils.storeInputStreamToFile(challengeStream);
      try {
        driver.get(TestUtils.URL_ROOT
            + "setup/");

        if (null != driver.findElement(By.name("submit_login"))) {
          login(driver);

          driver.get(TestUtils.URL_ROOT
              + "setup/");
        }

        final WebElement fileEle = driver.findElement(By.name("xmldocument"));
        fileEle.sendKeys(challengeFile.getAbsolutePath());

        if (forceRebuild) {
          final WebElement rebuildEle = driver.findElement(By.name("force_rebuild"));
          rebuildEle.click();
        }

        final WebElement reinitDB = driver.findElement(By.name("reinitializeDatabase"));
        reinitDB.click();

        final Alert confirmCreateDB = driver.switchTo().alert();
        LOGGER.info("Confirmation text: "
            + confirmCreateDB.getText());
        confirmCreateDB.accept();

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
   * Initialize the database using the given challenge descriptor.
   * 
   * @param selenium the test controller
   * @param challengeStream the challenge descriptor
   * @param forceRebuild if true, then force the database to be rebuilt
   * @throws IOException
   */
  public static void initializeDatabase(final Selenium selenium,
                                        final InputStream challengeStream,
                                        final boolean forceRebuild) throws IOException {
    try {
      Assert.assertNotNull(challengeStream);
      final File challengeFile = IntegrationTestUtils.storeInputStreamToFile(challengeStream);
      try {
        selenium.open(TestUtils.URL_ROOT
            + "setup/");
        selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

        if (selenium.isTextPresent("Login to FLL")) {
          login(selenium);

          selenium.open(TestUtils.URL_ROOT
              + "setup/");
          selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
        }

        selenium.attachFile("xmldocument", challengeFile.toURI().toURL().toString());
        if (forceRebuild) {
          selenium.click("force_rebuild");
        }
        selenium.click("reinitializeDatabase");
        Assert.assertTrue(selenium.getConfirmation()
                                  .matches("^This will erase ALL scores in the database fll \\(if it already exists\\), are you sure[\\s\\S]$"));
        selenium.waitForPageToLoad(WAIT_FOR_PAGE_TIMEOUT);

        final boolean success = selenium.isTextPresent("Successfully initialized database");
        Assert.assertTrue("Database was not successfully initialized", success);

        // setup user
        selenium.type("user", TEST_USERNAME);
        selenium.type("pass", TEST_PASSWORD);
        selenium.type("pass_check", TEST_PASSWORD);
        selenium.click("submit_create_user");
        selenium.waitForPageToLoad(WAIT_FOR_PAGE_TIMEOUT);
        final boolean userSuccess = selenium.isTextPresent("Successfully created user");
        Assert.assertTrue("Problem creating user", userSuccess);

        login(selenium);
      } finally {
        if (!challengeFile.delete()) {
          challengeFile.deleteOnExit();
        }
      }
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
   * Initialize a database from a zip file.
   * 
   * @param selenium the test controller
   * @param inputStream input stream that has database to load in it, this input
   *          stream is closed by this method upon successful completion
   * @throws IOException
   */
  public static void initializeDatabaseFromDump(final Selenium selenium,
                                                final InputStream inputStream) throws IOException {
    try {
      Assert.assertNotNull(inputStream);
      final File dumpFile = IntegrationTestUtils.storeInputStreamToFile(inputStream);
      try {
        selenium.open(TestUtils.URL_ROOT
            + "setup/");
        selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

        if (selenium.isTextPresent("Login to FLL")) {
          login(selenium);

          selenium.open(TestUtils.URL_ROOT
              + "setup/");
          selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
        }

        selenium.type("dbdump", dumpFile.getAbsolutePath());
        selenium.click("createdb");
        selenium.waitForPageToLoad(WAIT_FOR_PAGE_TIMEOUT);
        final boolean success = selenium.isTextPresent("Successfully initialized database");
        Assert.assertTrue("Database was not successfully initialized", success);

        // setup user
        selenium.type("user", TEST_USERNAME);
        selenium.type("pass", TEST_PASSWORD);
        selenium.type("pass_check", TEST_PASSWORD);
        selenium.click("submit_create_user");
        selenium.waitForPageToLoad(WAIT_FOR_PAGE_TIMEOUT);
        final boolean userSuccess = selenium.isTextPresent("Successfully created user");
        Assert.assertTrue("Problem creating user", userSuccess);

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
   * Initialize a database from a zip file.
   * 
   * @param selenium the test controller
   * @param inputStream input stream that has database to load in it, this input
   *          stream is closed by this method upon successful completion
   * @throws IOException
   */
  public static void initializeDatabaseFromDump(final WebDriver selenium,
                                                final InputStream inputStream) throws IOException {
    try {
      Assert.assertNotNull(inputStream);
      final File dumpFile = IntegrationTestUtils.storeInputStreamToFile(inputStream);
      try {
        selenium.get(TestUtils.URL_ROOT
            + "setup/");

        if (elementExists(selenium, By.name("submit_login"))) {
          login(selenium);

          selenium.get(TestUtils.URL_ROOT
              + "setup/");
        }

        final WebElement dbEle = selenium.findElement(By.name("dbdump"));
        dbEle.sendKeys(dumpFile.getAbsolutePath());

        final WebElement createEle = selenium.findElement(By.name("createdb"));
        createEle.click();

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

  public static void storeScreenshot(final Selenium selenium) throws IOException {
    final File baseFile = File.createTempFile("fll", null, new File("screenshots"));
    final File screenshot = new File(baseFile.getAbsolutePath()
        + ".png");
    selenium.captureScreenshot(screenshot.getAbsolutePath());
    LOGGER.error("Screenshot saved to "
        + screenshot.getAbsolutePath());

    final File htmlFile = new File(baseFile.getAbsolutePath()
        + ".html");
    final String html = selenium.getHtmlSource();
    final FileWriter writer = new FileWriter(htmlFile);
    writer.write(html);
    writer.close();
    LOGGER.error("HTML saved to "
        + htmlFile.getAbsolutePath());

  }

  public static void storeScreenshot(final WebDriver driver) throws IOException {
    final File baseFile = File.createTempFile("fll", null, new File("screenshots"));

    final File screenshot = new File(baseFile.getAbsolutePath()
        + ".png");
    if (driver instanceof TakesScreenshot) {
      final File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
      FileUtils.copyFile(scrFile, screenshot);
      LOGGER.error("Screenshot saved to "
          + screenshot.getAbsolutePath());
    } else {
      LOGGER.warn("Unable to get screenshot");
    }

    final File htmlFile = new File(baseFile.getAbsolutePath()
        + ".html");
    final String html = driver.getPageSource();
    final FileWriter writer = new FileWriter(htmlFile);
    writer.write(html);
    writer.close();
    LOGGER.error("HTML saved to "
        + htmlFile.getAbsolutePath());

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
  public static void login(final Selenium selenium) {
    selenium.open(TestUtils.URL_ROOT
        + "login.jsp");
    selenium.type("user", TEST_USERNAME);
    selenium.type("pass", TEST_PASSWORD);
    selenium.click("submit_login");
    selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
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

  /**
   * Add a team to a tournament.
   */
  public static void addTeam(final Selenium selenium,
                             final int teamNumber,
                             final String teamName,
                             final String organization,
                             final String division,
                             final String tournament) throws IOException {
    try {
      loadPage(selenium, TestUtils.URL_ROOT
          + "admin/index.jsp");

      selenium.click("link=Add a team");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      selenium.type("teamNumber", String.valueOf(teamNumber));
      selenium.type("teamName", teamName);
      selenium.type("organization", organization);
      selenium.click("id=division_text_choice");
      selenium.type("division_text", division);

      final String[] options = selenium.getSelectOptions("currentTournamentSelect");
      String tournamentID = null;
      for (int i = 0; i < options.length; ++i) {
        if (options[i].equals(tournament)) {
          tournamentID = options[i];
        }
      }
      Assert.assertNotNull("Could not find tournament with name: "
          + tournament, tournamentID);
      selenium.select("currentTournamentSelect", tournamentID);

      selenium.click("name=commit");

      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      Assert.assertTrue(selenium.isElementPresent("id=success"));

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
  public static void setTournament(final Selenium selenium,
                                   final String tournamentName) throws IOException {
    try {
      loadPage(selenium, TestUtils.URL_ROOT
          + "admin/index.jsp");

      final String[] options = selenium.getSelectOptions("currentTournamentSelect");
      String tournamentID = null;
      for (int i = 0; i < options.length; ++i) {
        if (options[i].endsWith("[ "
            + tournamentName + " ]")) {
          tournamentID = options[i];
        }
      }
      Assert.assertNotNull("Could not find tournament with name: "
          + tournamentName, tournamentID);

      selenium.select("currentTournamentSelect", tournamentID);

      selenium.click("change_tournament");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      Assert.assertTrue(selenium.isElementPresent("id=success"));
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
                                   final String tournamentName) throws IOException {
    try {
      loadPage(selenium, TestUtils.URL_ROOT
          + "admin/index.jsp");

      final WebElement currentTournament = selenium.findElement(By.id("currentTournamentSelect"));

      final Select currentTournamentSel = new Select(currentTournament);
      String tournamentID = null;
      for (final WebElement option : currentTournamentSel.getAllSelectedOptions()) {
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

}

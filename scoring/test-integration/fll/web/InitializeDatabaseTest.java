package fll.web;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.thoughtworks.selenium.Selenium;

import fll.TestUtils;
import fll.util.LogUtils;

/**
 * Test initializing the database via the web.
 */
public class InitializeDatabaseTest {

  private Selenium selenium;

  @Before
  public void setUp() throws Exception {
    LogUtils.initializeLogging();

    WebDriver driver = new FirefoxDriver();
    selenium = new WebDriverBackedSelenium(driver, TestUtils.URL_ROOT
        + "setup");
  }

  @Test
  public void testInitializeDatabase() throws IOException {
    final InputStream challengeStream = getClass().getResourceAsStream("data/challenge-ft.xml");
    IntegrationTestUtils.initializeDatabase(selenium, challengeStream, true);
  }
}

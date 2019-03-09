package fll.web;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.WebDriver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.LogUtils;

/**
 * Test initializing the database via the web.
 */
public class InitializeDatabaseTest {

  /**
   * Requirements for running tests.
   */
  @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by the JUnit framework")
  @Rule
  public RuleChain chain = RuleChain.outerRule(new IntegrationTestUtils.TomcatRequired());

  private static final Logger LOGGER = LogUtils.getLogger();

  private WebDriver selenium;

  @Before
  public void setUp() {
    LogUtils.initializeLogging();

    selenium = IntegrationTestUtils.createWebDriver();
  }

  @After
  public void tearDown() {
    selenium.quit();
  }

  @Test
  public void testInitializeDatabase() throws IOException, InterruptedException {
    try (InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml")) {
      IntegrationTestUtils.initializeDatabase(selenium, challengeStream);
    } catch (final IOException | RuntimeException | AssertionError e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }
}

package fll.web;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import fll.TestUtils;

/**
 * Test initializing the database via the web.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class InitializeDatabaseTest {

  /**
   * @param selenium browser driver
   * @param seleniumWait wait for elements
   * @throws IOException on test failure
   * @throws InterruptedException on test failure
   */
  @Test
  public void testInitializeDatabase(final WebDriver selenium,
                                     final WebDriverWait seleniumWait)
      throws IOException, InterruptedException {
    try (InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml")) {
      IntegrationTestUtils.initializeDatabase(selenium, seleniumWait, challengeStream);
    } catch (final IOException | RuntimeException | AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }
}

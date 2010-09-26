package fll.web;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.thoughtworks.selenium.SeleneseTestCase;

import fll.util.LogUtils;

/**
 * Test initializing the database via the web.
 */
public class InitializeDatabaseTest extends SeleneseTestCase {

  @Override
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    super.setUp("http://localhost:9080/setup");
  }

  @Test
  public void testInitializeDatabase() throws IOException {
    final InputStream challengeStream = getClass().getResourceAsStream("data/challenge-ft.xml");
    IntegrationTestUtils.initializeDatabase(selenium, challengeStream, true);
  }
}

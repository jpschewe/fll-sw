package fll.web;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.selenium.SeleneseTestCase;

import fll.TestUtils;
import fll.util.LogUtils;

/**
 * Test initializing the database via the web.
 */
public class InitializeDatabaseTest extends SeleneseTestCase {

  @Before
  @Override
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    super.setUp(TestUtils.URL_ROOT + "setup");
  }

  @Test
  public void testInitializeDatabase() throws IOException {
    final InputStream challengeStream = getClass().getResourceAsStream("data/challenge-ft.xml");
    IntegrationTestUtils.initializeDatabase(selenium, challengeStream, true);
  }
}

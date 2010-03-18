package fll.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.thoughtworks.selenium.SeleneseTestCase;
import com.thoughtworks.selenium.Selenium;

/**
 * Test initializing the database via the web.
 */
public class InitializeDatabaseTest extends SeleneseTestCase {

  private static final Logger LOGGER = Logger.getLogger(InitializeDatabaseTest.class);

  @Override
  public void setUp() throws Exception {
    super.setUp("http://localhost:9080/setup");
  }

  @Test
  public void testInitializeDatabase() throws IOException {
    selenium.open("http://localhost:9080/fll-sw/setup/");

    final InputStream challengeStream = getClass().getResourceAsStream("data/challenge-ft.xml");
    initializeDatabase(selenium, challengeStream, true);
  }

  /**
   * Initialize the database using the given challenge descriptor.
   * 
   * @param selenium the test controller
   * @param challengeStream the challenge descriptor
   * @param forceRebuild if true, then force the database to be rebuilt
   * @throws IOException
   */
  public static void initializeDatabase(final Selenium selenium, final InputStream challengeStream, final boolean forceRebuild) throws IOException {

    assertNotNull(challengeStream);
    final File challengeFile = storeInputStreamToFile(challengeStream);
    try {
      selenium.type("xmldocument", challengeFile.getAbsolutePath());
      if (forceRebuild) {
        selenium.click("force_rebuild");
      }
      selenium.click("reinitializeDatabase");
      assertTrue(selenium.getConfirmation().matches("^This will erase ALL scores in the database fll \\(if it already exists\\), are you sure[\\s\\S]$"));
      selenium.waitForPageToLoad("60000");
      final boolean success = selenium.isTextPresent("Successfully initialized database");
      if (!success) {
        storeScreenshot(selenium);
      }
      assertTrue("Database was not successfully initialized", success);
    } finally {
      if (!challengeFile.delete()) {
        challengeFile.deleteOnExit();
      }
    }
  }

  public static void storeScreenshot(final Selenium selenium) throws IOException {
    final File screenshot = File.createTempFile("fll", ".png", new File("screenshots"));
    selenium.captureScreenshot(screenshot.getAbsolutePath());
    LOGGER.error("Screenshot saved to "
        + screenshot.getAbsolutePath());
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
}

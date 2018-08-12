/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import fll.Launcher;
import fll.util.FLLInternalException;

/**
 * Send the SSL certificate to the user.
 */
@WebServlet("/fll-sw.crt")
public class DownloadSslCert extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final String tomcatBaseStr = System.getProperty("catalina.base");
    if (null == tomcatBaseStr) {
      throw new FLLInternalException("Unable to find base directory of webserver");
    }

    final Path tomcatBase = Paths.get(tomcatBaseStr);
    final Path keystoreFile = tomcatBase.resolve("conf").resolve(Launcher.KEYSTORE_FILENAME);
    if (!Files.exists(keystoreFile)) {
      throw new FLLInternalException("Unable to find tomcat keystore");
    }

    try {
      final KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (InputStream in = Files.newInputStream(keystoreFile)) {
        keyStore.load(in, CertificateUtils.KEYSTORE_PASSWORD.toCharArray());
      } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
        throw new FLLInternalException("Error loaoding keystore: "
            + e.getMessage(), e);
      }

      response.reset();
      response.setContentType("application/x-x509-ca-cert");
      response.setHeader("Content-Disposition", "filename=fll-sw.crt");

      final Certificate cert = keyStore.getCertificate(CertificateUtils.CERTIFICATE_ALIAS);
      try (JcaPEMWriter pw = new JcaPEMWriter(response.getWriter())) {
        pw.writeObject(cert);
      }
    } catch (final KeyStoreException e) {
      throw new FLLInternalException("Cannot find parser for keystore", e);
    }

  }

}


/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

/**
 * 
 */
public class SelfSignedCertificate {

  private static final String CERTIFICATE_ALIAS = "tomcat";

  private static final String CERTIFICATE_ALGORITHM = "RSA";

  private static final String CERTIFICATE_DN = "O=FLL-SW, ST=MN, C=US";

  private static final String CERTIFICATE_NAME = "keystore.test";

  private static final int CERTIFICATE_BITS = 4096;

  static {
    // adds the Bouncy castle provider to java security
    Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    SelfSignedCertificate signedCertificate = new SelfSignedCertificate();
    signedCertificate.createCertificate();
  }

  @SuppressWarnings("deprecation")
  private X509Certificate createCertificate() throws Exception {
    X509Certificate cert = null;
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CERTIFICATE_ALGORITHM);
    keyPairGenerator.initialize(CERTIFICATE_BITS, new SecureRandom());
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

//    var subjectAlternativeNames = new Asn1Encodable[]
//        {
//            new GeneralName(GeneralName.DnsName, "server"),
//            new GeneralName(GeneralName.DnsName, "server.mydomain.com")
//        };
//    var subjectAlternativeNamesExtension = new DerSequence(subjectAlternativeNames);
//    certificateGenerator.AddExtension(
//        Extension.subjectAlternativeName.Id, false, subjectAlternativeNamesExtension);
    
    // GENERATE THE X509 CERTIFICATE
    X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
    v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
    v3CertGen.setIssuerDN(new X509Principal(CERTIFICATE_DN));
    v3CertGen.setNotBefore(new Date(System.currentTimeMillis()
        - 1000L
            * 60
            * 60
            * 24));
    v3CertGen.setNotAfter(new Date(System.currentTimeMillis()
        + (1000L
            * 60
            * 60
            * 24
            * 365
            * 10)));
    v3CertGen.setSubjectDN(new X509Principal(CERTIFICATE_DN));
    v3CertGen.setPublicKey(keyPair.getPublic());
    v3CertGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
    cert = v3CertGen.generateX509Certificate(keyPair.getPrivate());
    
    saveCert(cert, keyPair.getPrivate());
    
    return cert;
  }

  private void saveCert(X509Certificate cert,
                        PrivateKey key)
      throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, null);
    keyStore.setKeyEntry(CERTIFICATE_ALIAS, key, "changit".toCharArray(),
                         new java.security.cert.Certificate[] { cert });
    File file = new File(".", CERTIFICATE_NAME);
    keyStore.store(new FileOutputStream(file), "changeit".toCharArray());
  }
}

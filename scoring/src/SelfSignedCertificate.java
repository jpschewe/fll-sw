
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
import java.util.Calendar;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

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

  private X509Certificate createCertificate() throws Exception {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CERTIFICATE_ALGORITHM);
    keyPairGenerator.initialize(CERTIFICATE_BITS, new SecureRandom());
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();

    // var subjectAlternativeNames = new Asn1Encodable[]
    // {
    // new GeneralName(GeneralName.DnsName, "server"),
    // new GeneralName(GeneralName.DnsName, "server.mydomain.com")
    // };
    // var subjectAlternativeNamesExtension = new
    // DerSequence(subjectAlternativeNames);
    // certificateGenerator.AddExtension(
    // Extension.subjectAlternativeName.Id, false,
    // subjectAlternativeNamesExtension);

    // GENERATE THE X509 CERTIFICATE
    final Calendar notBefore = Calendar.getInstance();
    notBefore.add(Calendar.HOUR_OF_DAY, -1);
    final Calendar notAfter = (Calendar) notBefore.clone();
    notAfter.add(Calendar.YEAR, 1);
    final X500Name issuer = new X500Name(CERTIFICATE_DN);
    final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
    final X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(issuer, BigInteger.ONE, notBefore.getTime(),
                                                                            notAfter.getTime(), issuer, publicKeyInfo);

    final ContentSigner signer = new JcaContentSignerBuilder("SHA1WithRSA").setProvider(new BouncyCastleProvider())
                                                                           .build(keyPair.getPrivate());
    final X509CertificateHolder holder = v3CertGen.build(signer);
    final X509Certificate cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                                                                  .getCertificate(holder);

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


/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.stream.IntStream;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fll.util.LogUtils;
import fll.web.WebUtils;

/**
 * 
 */
public class SelfSignedCertificate {

  private static Logger LOGGER = LoggerFactory.getLogger(SelfSignedCertificate.class);

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
  public static void main(final String[] args) throws Exception {
    LogUtils.initializeLogging();

    final SelfSignedCertificate signedCertificate = new SelfSignedCertificate();
    signedCertificate.createCertificate();
    System.out.println("Finished");
  }

  private X509Certificate createCertificate() throws Exception {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CERTIFICATE_ALGORITHM);
    keyPairGenerator.initialize(CERTIFICATE_BITS, new SecureRandom());
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();

    final Calendar notBefore = Calendar.getInstance();
    notBefore.add(Calendar.HOUR_OF_DAY, -1);
    final Calendar notAfter = (Calendar) notBefore.clone();
    notAfter.add(Calendar.YEAR, 1);
    final X500Name issuer = new X500Name(CERTIFICATE_DN);
    final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
    final X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(issuer, BigInteger.ONE, notBefore.getTime(),
                                                                            notAfter.getTime(), issuer, publicKeyInfo);

    final ASN1EncodableVector names = new ASN1EncodableVector();
    names.add(new GeneralName(GeneralName.dNSName, "localhost"));

    names.add(new GeneralName(GeneralName.dNSName, "127.0.0.1"));
    names.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));

    addNamesForMinnesotaLaptops(names);

    addAllLocalNames(names);

    final DERSequence subjectAlternativeNames = new DERSequence(names);
    v3CertGen.addExtension(Extension.subjectAlternativeName, false, subjectAlternativeNames);

    final ContentSigner signer = new JcaContentSignerBuilder("SHA1WithRSA").setProvider(new BouncyCastleProvider())
                                                                           .build(keyPair.getPrivate());
    final X509CertificateHolder holder = v3CertGen.build(signer);
    final X509Certificate cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                                                                  .getCertificate(holder);

    saveCert(cert, keyPair.getPrivate());

    return cert;
  }

  private static void addNamesForMinnesotaLaptops(final ASN1EncodableVector names) {
    IntStream.rangeClosed(1, 99).forEach(i -> {
      final String nameLocal = String.format("fll-%02d.local", i);
      names.add(new GeneralName(GeneralName.dNSName, nameLocal));
    });
  }

  /**
   * Find all of the IP addresses for this system and resolve them to names if
   * possible. Then add both the IP address and the name for the list of names for
   * the certificate.
   */
  private static void addAllLocalNames(final ASN1EncodableVector names) {
    for (final InetAddress address : WebUtils.getAllIPs()) {
      final String addrStr = WebUtils.getHostAddress(address);

      names.add(new GeneralName(GeneralName.iPAddress, addrStr));
      names.add(new GeneralName(GeneralName.dNSName, addrStr));

      if (!address.isLoopbackAddress()) {

        // check for a name
        try {
          final String name = org.xbill.DNS.Address.getHostName(address);
          names.add(new GeneralName(GeneralName.dNSName, name));
        } catch (final UnknownHostException e) {
          LOGGER.trace("Could not resolve IP: {}", addrStr, e);
        }
      } // not loopback
    } // foreach IP
  }

  private void saveCert(final X509Certificate cert,
                        final PrivateKey key)
      throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, null);
    keyStore.setKeyEntry(CERTIFICATE_ALIAS, key, "changit".toCharArray(),
                         new java.security.cert.Certificate[] { cert });
    File file = new File(".", CERTIFICATE_NAME);
    keyStore.store(new FileOutputStream(file), "changeit".toCharArray());
  }
}

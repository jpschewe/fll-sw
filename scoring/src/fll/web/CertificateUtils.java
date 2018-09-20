/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fll.util.FLLInternalException;

/**
 * Utilities for working with certificates.
 */
public class CertificateUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(CertificateUtils.class);

  /**
   * Alias for the SSL certificate inside the keystore.
   */
  public static final String CERTIFICATE_ALIAS = "tomcat";

  private static final String CERTIFICATE_ALGORITHM = "RSA";

  private static final String CERTIFICATE_DN = "O=FLL-SW, ST=MN, C=US";

  /**
   * Password used for the tomcat keystore.
   */
  public static final String KEYSTORE_PASSWORD = "changeit";

  private static final int CERTIFICATE_BITS = 4096;

  static {
    // adds the Bouncy castle provider to java security
    Security.addProvider(new BouncyCastleProvider());
  }

  private CertificateUtils() {
  }

  /**
   * Create a certificate for this host and all common hostnames for Minnesota
   * servers. The generated certificate is good for 1 month. This should balance
   * the ability to run a tournament and keep from exposing users's computers to
   * the threat of another entity using this certificate for evil means.
   * 
   * @param keystoreFilename where to store the certificate, this creates a new
   *          keystore
   * @throws NoSuchAlgorithmException If the certificate algorithm cannot be found
   * @throws OperatorCreationException If there is an error creating the
   *           certificate
   * @throws CertificateException if there is a general error creating the
   *           certificate
   * @throws IOException If there is a problem writing the keystore
   * @throws KeyStoreException If there is a problem writing the keystore
   */
  public static void createAndStoreCertificate(final Path keystoreFilename)
      throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, KeyStoreException, IOException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CERTIFICATE_ALGORITHM);
    keyPairGenerator.initialize(CERTIFICATE_BITS, new SecureRandom());
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();

    final Calendar notBefore = Calendar.getInstance();
    notBefore.add(Calendar.HOUR_OF_DAY, -1);
    final Calendar notAfter = (Calendar) notBefore.clone();
    notAfter.add(Calendar.MONTH, 1);
    final X500Name issuer = new X500Name(CERTIFICATE_DN);
    final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
    // use the current time as the serial number to avoid collisions when generating
    // new certificates
    final X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(issuer,
                                                                            new BigInteger(String.valueOf(System.currentTimeMillis())),
                                                                            notBefore.getTime(), notAfter.getTime(),
                                                                            issuer, publicKeyInfo);

    v3CertGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

    final ASN1EncodableVector names = new ASN1EncodableVector();

    // always add localhost
    names.add(new GeneralName(GeneralName.dNSName, "localhost"));
    names.add(new GeneralName(GeneralName.dNSName, "127.0.0.1"));
    names.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));

    addNamesForMinnesotaLaptops(names);

    addAllLocalNames(names);

    final DERSequence subjectAlternativeNames = new DERSequence(names);
    v3CertGen.addExtension(Extension.subjectAlternativeName, false, subjectAlternativeNames);

    final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").setProvider(new BouncyCastleProvider())
                                                                             .build(keyPair.getPrivate());
    final X509CertificateHolder holder = v3CertGen.build(signer);
    final X509Certificate cert = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider())
                                                                  .getCertificate(holder);

    saveCert(keystoreFilename, cert, keyPair.getPrivate());
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
          LOGGER.trace("Could not resolve IP: "
              + addrStr, e);
        }
      } // not loopback
    } // foreach IP
  }

  /**
   * Create a keystore and put the certificate in it.
   * 
   * @param keystoreFile where to create the keystore
   * @param cert the certificate to store
   * @param key the private key to store
   * @throws KeyStoreException
   * @throws IOException if there is a problem writing the keystore
   * @throws CertificateException if there is a problem with the certificate
   * @throws NoSuchAlgorithmException if the certificate algorithm cannot be found
   */
  private static void saveCert(final Path keystoreFile,
                               final X509Certificate cert,
                               final PrivateKey key)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    final KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, null);
    keyStore.setKeyEntry(CERTIFICATE_ALIAS, key, KEYSTORE_PASSWORD.toCharArray(),
                         new java.security.cert.Certificate[] { cert });

    try (OutputStream out = Files.newOutputStream(keystoreFile)) {
      keyStore.store(out, KEYSTORE_PASSWORD.toCharArray());
    }
    LOGGER.debug("Wrote keystore to "
        + keystoreFile.toString());
  }

  /**
   * @param keystoreFile the path to the keystore used by the webserver
   * @return the SSL certificate used for the web server
   * @throws KeyStoreException see {@link KeyStore#getInstance(String)} using
   *           "PKCS12" for the type and see
   *           {@link KeyStore#getCertificate(String)}
   */
  public static Certificate getCertificate(final Path keystoreFile) throws KeyStoreException {
    final KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try (InputStream in = Files.newInputStream(keystoreFile)) {
      keyStore.load(in, CertificateUtils.KEYSTORE_PASSWORD.toCharArray());
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      throw new FLLInternalException("Error loaoding keystore: "
          + e.getMessage(), e);
    }

    final Certificate cert = keyStore.getCertificate(CertificateUtils.CERTIFICATE_ALIAS);
    return cert;
  }

  /**
   * @param cert the certificate to check
   * @return if the current date is within the validity dates of the certificate
   * @see X509Certificate#getNotAfter()
   * @see X509Certificate#getNotBefore()
   */
  public static boolean isCertificateDateValid(final X509Certificate cert) {
    try {
      cert.checkValidity();
      return true;
    } catch (CertificateExpiredException | CertificateNotYetValidException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Invalid cert: "
            + e.getMessage(), e);
      }
      return false;
    }
  }

  /**
   * Documented at {@link X509Certificate#getSubjectAlternativeNames()}
   */
  private static final int IP_GENERAL_NAME_TYPE = 7;

  private static Collection<String> getCertificateIps(final X509Certificate cert) throws CertificateParsingException {
    final Collection<List<?>> altNames = cert.getSubjectAlternativeNames();
    if (null == altNames) {
      return Collections.emptyList();
    }

    return altNames.stream().filter(e -> e.get(0).equals(IP_GENERAL_NAME_TYPE)).map(e -> e.get(1))
                   .filter(e -> e instanceof String).map(e -> (String) e).collect(Collectors.toList());
  }

  /**
   * @param keystoreFile the path to the keystore to check
   * @return if the certificate store contains all of the IP addresses of this
   *         system and is not expired.
   * @throws CertificateParsingException if there is a problem parsing the found certificate
   * @throws KeyStoreException if there is a problem reading the keystore 
   */
  public static boolean checkCertificateStore(final Path keystoreFile) throws CertificateParsingException, KeyStoreException {
    // currently valid?
    final Certificate cert = getCertificate(keystoreFile);
    if (cert instanceof X509Certificate) {
      final X509Certificate cert509 = (X509Certificate) cert;
      if (!isCertificateDateValid(cert509)) {
        return false;
      }

      final Collection<String> certificateIps = getCertificateIps(cert509);
      for (final InetAddress address : WebUtils.getAllIPs()) {
        final String addrStr = WebUtils.getHostAddress(address);
        if (!certificateIps.contains(addrStr)) {
          return false;
        }
      }

      return true;

    } else {
      LOGGER.warn("Checking non-x509 certificate, cannot validate, assuming valid");
      return true;
    }
  }

}

# Android
For this to work it is likely that you will need to have a password or pin code set on your tablet. If you do not have one set, you will be prompted during these steps if one is required.

## Chrome and Silk
In Minnesota this includes both the tablets with grey cases and those with colorful cases.

1. Click on the link to download the certificate
1. If prompted asking if it should be downloaded again say yes
1. Enter pin or fingerprint if prompted
1. Name the certificate 'FLL-SW'
1. Leave used for at "VPN and apps"
1. Click Ok

If you get an error that the certificate file could not be installed because it could not be read, do the following.
This will most likely happen on a Google Nexus 7 (in Minnesota the grey tablets).

1. Open Settings
1. Click on Security
1. Under Credentials click on Install from Storage
1. Click Downloads
1. Click on the most recent fll-sw.crt file (closest to the top)
1. Name the certificate 'FLL-SW'
1. Leave used for at "VPN and apps"
1. Click Ok
 

# IOS - iPhone, iPad

Not supported at this time. You should use the non-secure version of the app.

## Firefox

Firefox on Android isn't supported. The instructions below *should* work ,but don't seem to. Continue at your own risk.

1. Click on the link to download the certificate
1. A dialog will appear asking where to trust this certificate
1. Click Trust this certificate to identify websites
1. Click OK

When visiting the site you may get an error about a self-signed certificate. If you do, follow the instructions below:
1. Click "I Understand the Risks"
1. Click "Add permanent exception"


# Firefox on laptops

1. Click on the link to download the certificate
1. A dialog will appear asking where to trust this certificate
1. Click Trust this certificate to identify websites
1. Click OK

# IE on laptops

1. Click on the link to download the certificate
1. Double click on the downloaded file
1. Choose to store the certificate for the local user
1. Choose where to install the certificate
1. Choose Trusted Root Authorities
1. OK

# Chrome on laptops

## Windows

Follow the instructions for IE

## Linux

1. Click on the link to download the certificate
1. Goto Chrome Settings
1. Click on Advanced
1. Click on Manage Certificates
1. Click on Authorities
1. Click Import
1. Select the downloaded file
1. Select Trust this certificate to identify websites
1. OK

## Mac

1. Click on the link to download the certificate
1. Open up Keychain Access. You can get to it from Applications/Utilities/Keychain Access.app.
1. Drag your certificate into Keychain Access.
1. Go into the Certificates section and locate the certificate you just added
1. Double click on it, enter the trust section and under "When using this certificate" select "Always Trust"

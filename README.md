# ping-api

## Introducing PING

PING is the national platform for nautical information developed and implemented by Shom. The Shom is France's national hydrographic service. It is the NAVAREA II area coordinator of the World-Wide Navigational Warning Service (WWNWS) and the national nautical information coordinator. The platform is co-published by Shom and the Direction Générale des Affaires Maritimes, de la Pêche et de l'Aquaculture. Its development was co-financed by the European Union (Interreg Mediterranean MED OSMoSIS Project, FEAMP). The platform has been operational since April 2024.


The PING web platform is a shared information system that facilitates the transmission, formatting, digitization and posting on the Internet of nautical information. Nautical information is digitized for wide distribution and integration into user systems (ship navigation systems, shore-based service systems, user systems, etc.). Nautical information data is geolocated, formatted and supplied in accordance with applicable standards and recommendations (in particular those developed by the IHO as part of WWNWS, such as S-124), so that it is interoperable and accessible using standardized exchange protocols.


PING presents a portal for users and data services for systems.


The users of the portal are the public, who can consult navigational warnings, notices to mariners and maritime zone regulations (with the option of subscribing) and transmit new information (contributor), and the staff of contributing departments or those responsible for producing official nautical information (navigational warnings and notices to mariners, updates to nautical charts and publications, spatialized maritime regulations). PING includes tools for producing navigational warnings, notices to mariners and spatialized regulations.


There are several types of data services. Some of these data services use Geoserver to deliver WMS or WFS streams, while others are REST APIs, notably to deliver S-124 data in accordance with the SECOM standard.


PING has specific interfaces with broadcast systems and other national systems (REST API or Sftp file transfer).


Some optional notions of S-124 data model have not been implemented: NavwarnPreamble/navwarnTitle, NavwarnPreamble/affectedChartPublications, SpatialQuality, TextPlacement, NavwarnPart/featureName, NavwarnPart/featureReference and NavwarnPart/restriction


For full details of PING features, please refer to the portal home page at https://portail.ping-info-nautique.fr/, to its Data Services section and to the portal's general terms and conditions of use.


## PING projects

PING consists of several Java Spring Boot and Angular projects. The two projects that have been released as open source and are available are as follows:
- ping-api: the back end that interacts with the database and various third-party dependencies (Kafka data bus, Keycloak, email sending, etc.), and responds to requests from ping-web
- ping-web: the front end that will interact directly with ping-api, as well as with Keycloak for the user login part

/!\ Not all projects are available as open source, so it is important to note that certain features/APIs will not be available /!\


## Local usage

### Start database

From DevBow/WSL, go to `ping-api` folder, then :

```
cd db
docker-compose up -d

```

### Windows configuration

If forge SSL certificate is not recognized par root CA, you have to use following system variable in order to build project with `maven` :

```
MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
```


## Application configuration

Configuration set is available, for each environment (local, qlf, prod), and can be added to JAVA_OPTS : 

```
-Dserver.port=8081                                              # configuration port

-Dspring.datasource.url=jdbc:postgresql://ip:port/ping_api      # configuration base de données url
-Dspring.datasource.username=ping_api                           # configuration base de données utilisateur
-Dspring.datasource.password=ping_api_pwd                       # configuration base de données mot de passe

-Dping.cors.allowed-origins=https://##PING_PORTAL_URL##         # à configurer avec l'url du portail (attention, pas de / à la fin)
-Dping.upload-folder=/DATA/ping/api/attachments                 # dossier d'uploads des pièces jointes => dossier à créer manuellement
-Dping.generated-shapefile-folder=/DATA/ping/api/shapefiles     # dossier pour les shapefiles => dossier à créer manuellement
 
-Dping.keycloak.server-url=https://##PING_SERVICES_URL##/auth   # à configurer avec l'url du service auth
-Dping.keycloak.use-proxy=true                                  # à configurer à true ou false en fonction de si le flux passe par un proxy entre ping-api et keycloak
-Dping.keycloak.proxy-host=http://proxy.internet                # à configurer avec l'url du proxy (le cas échéant)
-Dping.keycloak.proxy-port=3128                                 # à configurer avec le port du proxy (le cas échéant)
-Dkeycloak.ssl-required=none                                    # à mettre uniquement si le serveur-url est en http et non en https
 
-Dkafka.bootstrapAddress=server:port                            # point d'accès kafka (ex : localhost:9092)
 
-Dlogging.level.org.springframework.security=DEBUG              # configuration éventuelle pour du DEBUG (à ne pas laisser en DEBUG en production)
-Dlogging.level.org.keycloak=DEBUG                              # configuration éventuelle pour du DEBUG (à ne pas laisser en DEBUG en production)

```

## SECOM and S100 certificates and keystore
SECOM and S124 v2 was set up based on version 1.5.0

This section describes how to create certificate, sign certificate, store certificate and private key in a keystore.
Two root certificates are needed to sign the SECOM and S100 certificates.
s100_root is used to sign S100 certificates (SHOM signature) and ping_root is used to sign SECOM certificates (PING_SECOM signature).

Only one keystore is used to store the certificates and private keys. (secom_{{env}}_keystore.jks).

# create empty keystore

We choose to use ecdsa as keyalg to create for more security. There is no incovenience to use ecdsa instead of dsa to store the certificates and private keys 
even if the certificates are signed with dsa for SECOM and ecdsa for S100.
The keystore is create in certificates directory. You must create the directory before running the command if it does not exist.

Once all certificates are created, signed and imported in the keystore, you must copy the keystore to src/main/resources/secom/ to be used by the application : 
```
mv certificates/secom_{{env}}_keystore.jks src/main/resources/secom/
```


```
keytool -genkey -alias dummy -keyalg EC -storetype JKS -keystore secom_keystore.jks -keysize 384 -validity 365 -storepass S3c0mP@ssw0rd -sigalg SHA384withECDSA
```

Use following:
```
Enter the distinguished name. Provide a single dot (.) to leave a sub-component empty or press ENTER to use the default value in braces.
What is your first and last name?
[Unknown]:  PING
What is the name of your organizational unit?
[Unknown]:  WL PING
What is the name of your organization?
[Unknown]:  WL
What is the name of your City or Locality?
[Unknown]:
What is the name of your State or Province?
[Unknown]:  France
What is the two-letter country code for this unit?
[Unknown]:  FR
Is CN=PING, OU=WL PING, O=WL, L=Unknown, ST=France, C=FR correct?
[no]:  yes


Generating 384 bit ECDSA key pair and self-signed certificate (SHA2_384_WITH_ECDSA) with a validity of 365 days
for: CN=PING, OU=WL PING, O=WL, L=Unknown, ST=France, C=FR
Enter key password for <dummy>
(RETURN if same as keystore password): DumMyP@ssw0rd
```

After creating the keystore, you need to create the root certificates and the CSR for SHOM and PING SECOM.
=> See SECOM.md & S100.md for more details (be careful, the commands are different for SECOM and S100) and be careful to adapt path to keystore to store the certificates and private keys.

After all the certificates are created, signed and imported in the keystore, you must copy the keystore to src/main/resources/secom/ to be used by the application.

---

# Test mail process

---
To test the evolution applied to the mail, you'll need to configure your local workspace.

## Installation of `GreenMail`
```
docker run -t -i -p 3025:3025 -p 3110:3110 -p 3143:3143 -p 3465:3465 -p 3993:3993 -p 3995:3995 -p 8086:8080 greenmail/standalone:1.6.5
```
  
## Installation of mail client :

You need to install, **on the devbox**, a utility like thunderbird to receive your mail sent from the application.
(or another mail client capable of connecting to an IMAP server)

When mail client was configured :
 
 - Test installation between greenmail and mail client, with telnet
```
telnet localhost 3025
```
 - Send mail
    ```
    HELO localhost
    MAIL FROM:<test@localhost>
    RCPT TO:<you-mail-configurted>
    DATA
    Subject: Test Subject
    This is a test email sent from telnet.
    .
    QUIT
    ```
  - and checked if your email has been received by your mail client

### Pull project `shom2-ping-email-producer`:
 - You need to pull the **shom2-ping-email-producer** project and Launched on !

### add port on `shom2-ping-auth`:
On logs to container **ping_auth_mail** retrieve the container's listening port.
  
On project `shom2-ping-auth`
we'll need to add port on **docker-compose.yml**
in **auth_mail** section
  ```
  auth_mail:      // section
  container_name: containerName
  image: imageName
  ports:
    - SomePort....
    - "1080:1080" // add here
  ```

The algorithm used for the SECOM certificate is DSA with SHA256.
We wil use the keystore already created "secom_{{env}}_keystore.jks" to store the certificates and private keys.

You must first create secom_labnum_certificates directory to store the certificates and private keys if it does not exist.
```
cd certificates
mkdir secom_labnum_certificates
cd secom_labnum_certificates
```

# create SECOM ROOT trusted certificate
```
openssl dsaparam -out dsaparam.txt 2048
openssl req -x509 -sha256 -nodes -days 365 -newkey dsa:dsaparam.txt -keyout secom_root.key -out secom_root.crt
```

Use following:
```
Country Name (2 letter code) [AU]:XX
State or Province Name (full name) [Some-State]:XXXXX
Locality Name (eg, city) []:
Organization Name (eg, company) [Internet Widgits Pty Ltd]:xxxx
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:PING
Email Address []:
```

# import trusted PING ROOT certificate as CA certificate in keystore
```
keytool -import -trustcacerts -alias secom_root -keystore ../secom_{{env}}_keystore.jks -file secom_root.crt -storepass SECOM_{{env}}_KEYSTORE_PASSWORD
```

# create CSR for PING SECOM
```
openssl dsaparam -out ping_secom-param.txt 2048
openssl req -out ping_secom.csr -new -newkey dsa:ping_secom-param.txt -nodes -keyout ping_secom_private.key
```


Use following:
```
Country Name (2 letter code) [AU]:XX
State or Province Name (full name) [Some-State]:XXXXX
Locality Name (eg, city) []:
Organization Name (eg, company) [Internet Widgits Pty Ltd]:PING SECOM
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:PING SECOM
Email Address []:

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:xxxxxx
An optional company name []:
```

# split public key from private key
```
openssl dsa -outform pem -in ping_secom_private.key -out ping_secom_public.txt -pubout
```

# create signed PING SECOM certificate by CA from CSR
```
openssl x509 -req -in ping_secom.csr -sha256 -CA secom_root.crt -CAkey secom_root.key -CAcreateserial -out ping_secom.crt
```

# check PING SECOM certificate against CA (PING ROOT)
```
openssl verify -verbose -CAfile secom_root.crt ping_secom.crt
```

# import PING SECOM certificate in keystore
```
keytool -import -alias ping_secom -file ping_secom.crt -keystore ../secom_{{env}}_keystore.jks -storepass SECOM_{{env}}_KEYSTORE_PASSWORD
```

# import private key to keystore
```
openssl pkcs12 -export -in secom_root.crt -inkey secom_root.key -out secom_root.p12 -name secom_root -CAfile secom_root.crt -caname secom_root
keytool -importkeystore -srckeystore secom_root.p12 -srcstoretype PKCS12 -destkeystore ../secom_{{env}}_keystore.jks -deststoretype JKS -srcstorepass SECOM_{{env}}_KEYSTORE_PASSWORD -deststorepass SECOM_{{env}}_KEYSTORE_PASSWORD
```

```
openssl pkcs12 -export -in ping_secom.crt -inkey ping_secom_private.key -out ping_secom.p12 -name ping_secom -CAfile secom_root.crt -caname secom_root
keytool -importkeystore -srckeystore ping_secom.p12 -srcstoretype PKCS12 -destkeystore ../secom_{{env}}_keystore.jks -deststoretype JKS -srcstorepass SECOM_{{env}}_KEYSTORE_PASSWORD -deststorepass SECOM_{{env}}_KEYSTORE_PASSWORD
```

# list certificates
```
keytool -list -rfc -keystore ../secom_{{env}}_keystore.jks -storepass SECOM_{{env}}_KEYSTORE_PASSWORD
```

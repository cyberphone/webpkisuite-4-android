/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.crypto;

import java.io.IOException;

public enum KeyUsageBits {

    DIGITAL_SIGNATURE ("digitalSignature"),
    NON_REPUDIATION   ("nonRepudiation"),
    KEY_ENCIPHERMENT  ("keyEncipherment"),
    DATA_ENCIPHERMENT ("dataEncipherment"),
    KEY_AGREEMENT     ("keyAgreement"),
    KEY_CERT_SIGN     ("keyCertSign"),
    CRL_SIGN          ("cRLSign"),
    ENCIPHER_ONLY     ("encipherOnly"),
    DECIPHER_ONLY     ("decipherOnly");

    String x509_name;  // Used in WebPKI protocols

    KeyUsageBits(String x509_name) {
        this.x509_name = x509_name;
    }


    public String getX509Name() {
        return x509_name;
    }


    public static KeyUsageBits getKeyUsageBit(String x509_name) throws IOException {
        for (KeyUsageBits kubit : values()) {
            if (kubit.x509_name.equals(x509_name)) {
                return kubit;
            }
        }
        throw new IOException("Bad KeyUsage bit: " + x509_name);
    }
}

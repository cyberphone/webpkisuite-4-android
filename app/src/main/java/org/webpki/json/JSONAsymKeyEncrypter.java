/*
 *  Copyright 2006-2024 WebPKI.org (https://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.json;

import java.security.PublicKey;

import org.webpki.crypto.KeyEncryptionAlgorithms;

/**
 * Initiator object for asymmetric key encryptions.
 */
public class JSONAsymKeyEncrypter extends JSONEncrypter {

    /**
     * Constructor for JCE based solutions.
     * @param publicKey Public key used for encrypting the key
     * @param keyEncryptionAlgorithm The algorithm used for encrypting the key
     */
    public JSONAsymKeyEncrypter(PublicKey publicKey,
                                KeyEncryptionAlgorithms keyEncryptionAlgorithm) {
        this.publicKey = publicKey;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
    }

    @Override
    void writeKeyData(JSONObjectWriter wr) {
        wr.setPublicKey(publicKey, algorithmPreferences);
    }
}

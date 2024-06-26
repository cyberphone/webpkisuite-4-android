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
package org.webpki.crypto;

import java.security.SecureRandom;

import org.webpki.util.Base64URL;

/**
 * Support class to {@link SecureRandom}.
 *
 */
public class CryptoRandom {

    private CryptoRandom() { }

    /**
     * Generates byte array with random data.
     * 
     * @param length Number of bytes
     * @return byteArray
     */
    public static byte[] generateRandom(int length) {
        byte[] random = new byte[length];
        new SecureRandom().nextBytes(random);
        return random;
    }

    /**
     * Generates a URL friendly encoded nonce.
     * <p>
     * Note: although the characters are derived from Bas64URL,
     * the string is NOT intended to be decoded!
     * </p>
     * @param length Number of characters
     * @return Encoded nonce
     */
    public static String generateURLFriendlyRandom(int length) {
        return Base64URL.encode(generateRandom(length)).substring(0, length);
    }
}

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
package org.webpki.sks;

import java.io.IOException;
import java.io.Serializable;

import java.math.BigInteger;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.KeyAlgorithms;
import org.webpki.crypto.KeyTypes;

public class KeySpecifier implements Serializable {
    private static final long serialVersionUID = 1L;

    byte[] parameters;

    KeyAlgorithms key_algorithm;

    public KeySpecifier(KeyAlgorithms key_algorithm) {
        this.key_algorithm = key_algorithm;
    }


    KeySpecifier(KeyAlgorithms key_algorithm, byte[] optional_parameter) throws IOException {
        this(key_algorithm);
        if (optional_parameter != null) {
            if (!key_algorithm.hasParameters()) {
                throw new IOException("Algorithm '" + key_algorithm.toString() + "' does not use a \"Parameters\"");
            }
            if (key_algorithm.getKeyType() == KeyTypes.RSA) {
                parameters = optional_parameter;
            } else {
                throw new IOException("Algorithm '" + key_algorithm.toString() + "' not fu implemented");
            }
        }
    }


    public KeySpecifier(KeyAlgorithms key_algorithm, long parameter) throws IOException {
        this(key_algorithm, BigInteger.valueOf(parameter).toByteArray());
    }


    public KeySpecifier(String uri, byte[] optional_parameters) throws IOException {
        this(KeyAlgorithms.getKeyAlgorithmFromId(uri, AlgorithmPreferences.SKS), optional_parameters);
    }


    public byte[] getParameters() throws IOException {
        return parameters;
    }


    public KeyAlgorithms getKeyAlgorithm() {
        return key_algorithm;
    }
}

/*
 *  Copyright 2006-2015 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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


public enum AsymEncryptionAlgorithms implements EncryptionAlgorithms
  {
    RSA_ES_PKCS_1_5        ("http://xmlns.webpki.org/sks/algorithm#rsa.es.pkcs1_5",
                            null,
                            "1.2.840.113549.1.1.1",
                            "RSA/ECB/PKCS1Padding"),

    RSA_OAEP_SHA1_MGF1P    ("http://xmlns.webpki.org/sks/algorithm#rsa.oaep.sha1.mgf1p",
                            "RSA-OAEP",
                            null,
                            "RSA/ECB/OAEPWithSHA1AndMGF1Padding"),

    RSA_OAEP_SHA256_MGF1P  ("http://xmlns.webpki.org/sks/algorithm#rsa.oaep.sha256.mgf1p",
                            "RSA-OAEP-256",
                            null,                            
                            "RSA/ECB/OAEPWithSHA256AndMGF1Padding"),

    RSA_RAW                ("http://xmlns.webpki.org/sks/algorithm#rsa.raw",
                            null,
                            null,
                            "RSA/ECB/NoPadding");

    private final String         sks_id;          // As (typically) expressed in protocols
    private final String         josename;        // Alternative JOSE name
    private final String         oid;             // As expressed in OIDs
    private final String         jcename;         // As expressed for JCE

    private AsymEncryptionAlgorithms (String sks_id, String josename, String oid, String jcename)
      {
        this.sks_id = sks_id;
        this.josename = josename;
        this.oid = oid;
        this.jcename = jcename;
      }


    @Override
    public boolean isSymmetric ()
      {
        return false;
      }


    @Override
    public boolean isMandatorySKSAlgorithm ()
      {
        return true;
      }


    @Override
    public String getJCEName ()
      {
        return jcename;
      }


    @Override
    public String getURI ()
      {
        return sks_id;
      }


    @Override
    public String getOID ()
      {
        return oid;
      }


    @Override
    public String getJOSEName ()
      {
        return josename;
      }


    public static AsymEncryptionAlgorithms getAlgorithmFromOID (String oid) throws IOException
      {
        for (AsymEncryptionAlgorithms alg : values ())
          {
            if (oid.equals (alg.oid))
              {
                return alg;
              }
          }
        throw new IOException ("Unknown algorithm: " + oid);
      }


    public static AsymEncryptionAlgorithms getAlgorithmFromID (String algorithm_id) throws IOException
      {
        for (AsymEncryptionAlgorithms alg : values ())
          {
            if (algorithm_id.equals (alg.sks_id) || algorithm_id.equals (alg.josename))
              {
                return alg;
              }
          }
        throw new IOException ("Unknown algorithm: " + algorithm_id);
      }
  }

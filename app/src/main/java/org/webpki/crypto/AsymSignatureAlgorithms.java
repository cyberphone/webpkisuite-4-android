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

public enum AsymSignatureAlgorithms implements SignatureAlgorithms
  {
    RSA_NONE     ("http://xmlns.webpki.org/sks/algorithm#rsa.pkcs1.none", null,
                  null,                    "NONEwithRSA",     null,                  true,  true),
        
    RSA_SHA1     ("http://www.w3.org/2000/09/xmldsig#rsa-sha1",           null,              
                  "1.2.840.113549.1.1.5",  "SHA1withRSA",     HashAlgorithms.SHA1,   true,  true),
        
    RSA_SHA256   ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",    "RS256",      
                  "1.2.840.113549.1.1.11", "SHA256withRSA",   HashAlgorithms.SHA256, true,  true),
        
    RSA_SHA384   ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha384",    "RS384",     
                  "1.2.840.113549.1.1.12", "SHA384withRSA",   HashAlgorithms.SHA384, true,  true),
        
    RSA_SHA512   ("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512",    "RS512",   
                  "1.2.840.113549.1.1.13", "SHA512withRSA",   HashAlgorithms.SHA512, true,  true),
        
    ECDSA_NONE   ("http://xmlns.webpki.org/sks/algorithm#ecdsa.none",     null,
                  null,                    "NONEwithECDSA",   null,                  true,  false),
        
    ECDSA_SHA256 ("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha256",  "ES256",  
                  "1.2.840.10045.4.3.2",   "SHA256withECDSA", HashAlgorithms.SHA256, true,  false),
        
    ECDSA_SHA384 ("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384",  "ES384",   
                  "1.2.840.10045.4.3.3",   "SHA384withECDSA", HashAlgorithms.SHA384, true,  false),
        
    ECDSA_SHA512 ("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512",  "ES512",   
                  "1.2.840.10045.4.3.4",   "SHA512withECDSA", HashAlgorithms.SHA512, true,  false);

    private final String sks_id;    // As (typically) expressed in protocols
    private final String josename;  // Alternative JOSE name
    private final String oid;       // As expressed in OIDs
    private final String jcename;   // As expressed for JCE
    private final HashAlgorithms digest_alg;
    private boolean sks_mandatory;  // If required in SKS
    private boolean rsa;            // RSA algorithm

    private AsymSignatureAlgorithms (String sks_id,
                                     String josename,
                                     String oid,
                                     String jcename,
                                     HashAlgorithms digest_alg,
                                     boolean sks_mandatory,
                                     boolean rsa)
      {
        this.sks_id = sks_id;
        this.josename = josename;
        this.oid = oid;
        this.jcename = jcename;
        this.digest_alg = digest_alg;
        this.sks_mandatory = sks_mandatory;
        this.rsa = rsa;
      }


    @Override
    public boolean isSymmetric ()
      {
        return false;
      }


    @Override
    public boolean isMandatorySKSAlgorithm ()
      {
        return sks_mandatory;
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


    public HashAlgorithms getDigestAlgorithm ()
      {
        return digest_alg;
      }


    public boolean isRSA ()
      {
        return rsa;
      }


    public static boolean testAlgorithmURI (String sks_id)
      {
        for (AsymSignatureAlgorithms alg : values ())
          {
            if (sks_id.equals (alg.sks_id))
              {
                return true;
              }
          }
        return false;
      }


    public static AsymSignatureAlgorithms getAlgorithmFromID (String algorithm_id) throws IOException
      {
        for (AsymSignatureAlgorithms alg : AsymSignatureAlgorithms.values ())
          {
            if (algorithm_id.equals (alg.sks_id) || algorithm_id.equals (alg.josename))
              {
                return alg;
              }
          }
        throw new IOException ("Unknown signature algorithm: " + algorithm_id);
      }
  }

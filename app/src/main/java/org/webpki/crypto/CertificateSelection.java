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

import java.util.Hashtable;

import java.security.cert.X509Certificate;


public class CertificateSelection
  {
    private Object provider;

    private Hashtable<String,X509Certificate> selection = new Hashtable<String,X509Certificate> ();


    public CertificateSelection (Object provider)
      {
        this.provider = provider;
      }


    public void addEntry (String key_alias, X509Certificate certificate)
      {
        selection.put (key_alias, certificate);
      }


    public Object getProvider ()
      {
        return provider;
      }


    public X509Certificate getCertificate (String key_alias)
      {
        return selection.get (key_alias);
      }


    public String[] getKeyAliases ()
      {
        return selection.keySet ().toArray (new String[0]);
      }

  }

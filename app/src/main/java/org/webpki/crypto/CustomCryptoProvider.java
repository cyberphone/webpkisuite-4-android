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

import java.security.Provider;
import java.security.Security;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bouncycastle loader
 */
public class CustomCryptoProvider
  {
    private static Logger logger = Logger.getLogger (CustomCryptoProvider.class.getCanonicalName ());

    private CustomCryptoProvider () {} // No instantiation

    private static boolean loadBouncyCastle (boolean insert_first, boolean require)
      {
        boolean loaded = false;
        try
          {
            Provider bc = (Provider) Class.forName ("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance ();
            if (Security.getProvider (bc.getName ()) == null)
              {
                try
                  {
                    if (insert_first)
                      {
                        Security.insertProviderAt (bc, 1);
                        logger.info ("BouncyCastle successfully inserted at position #1");
                      }
                    else
                      {
                        Security.addProvider (bc);
                        logger.info ("BouncyCastle successfully added to the list of providers");
                      }
                  }
                catch (Exception e)
                  {
                    logger.log (Level.SEVERE, "BouncyCastle didn't load");
                    throw new RuntimeException (e);
                  }
              }
            else
              {
                logger.info ("BouncyCastle was already loaded");
              }
            loaded = true;
          }
        catch (Exception e)
          {
            if (require)
              {
                logger.log (Level.SEVERE, "BouncyCastle was not found");
                throw new RuntimeException (e);
              }
            logger.info ("BouncyCastle was not found, continue anyway");
          }
        return loaded;
      }

    public static boolean conditionalLoad (boolean insert_first)
      {
        return loadBouncyCastle (insert_first, false);
      }

    public static void forcedLoad (boolean insert_first)
      {
        loadBouncyCastle (insert_first, true);
      }
  }

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
package org.webpki.json;

import java.io.IOException;
import java.io.Serializable;

import java.util.Hashtable;

/**
 * Stores {@link JSONDecoder} classes for automatic instantiation during parsing.
 * This is (sort of) an emulation of XML schema caches.
 * <p>
 * The cache system assumes that JSON documents follow a strict convention:<br>
 * &nbsp;<br><code>
 * &nbsp;&nbsp;{<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&quot;@context&quot;:&nbsp;&quot;</code><i>Message Context</i><code>&quot;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&quot;@qualifier&quot;:&nbsp;&quot;</code><i>Message Type Qualifier</i><code>&quot;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.&nbsp;&nbsp;&nbsp;</code><i>Arbitrary JSON Payload</i><code><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.<br>
 * &nbsp;&nbsp;}</code><p>
 * Note: <code>@qualifier</code> is only required if multiple objects share the same <code>@context</code>.<p>
 * A restriction imposed by this particular JSON processing model is that all properties must by default be read.
 * 
 */
public class JSONDecoderCache implements Serializable
  {
    private static final long serialVersionUID = 1L;

    /**
     * Emulation of XML namespace
     */
    public static final String CONTEXT_JSON = "@context";
    
    /**
     * Emulation of XML top-level element. Optional
     */
    public static final String QUALIFIER_JSON = "@qualifier";
    
    static final char CONTEXT_QUALIFIER_DIVIDER = '$';

    boolean check_for_unread = true;
    
    Hashtable<String,Class<? extends JSONDecoder>> class_map = new Hashtable<String,Class<? extends JSONDecoder>> ();

    public JSONDecoder parse (JSONObjectReader reader) throws IOException
      {
        String object_type_identifier = reader.getString (CONTEXT_JSON);
        if (reader.hasProperty (QUALIFIER_JSON))
          {
            object_type_identifier += CONTEXT_QUALIFIER_DIVIDER + reader.getString (QUALIFIER_JSON);
          }
        Class<? extends JSONDecoder> decoder_class = class_map.get (object_type_identifier);
        if (decoder_class == null)
          {
            throw new IOException ("Unknown JSONDecoder type: " + object_type_identifier);
          }
        try
          {
            JSONDecoder decoder = decoder_class.newInstance ();
            decoder.root = reader.root;
            decoder.readJSONData (reader);
            if (check_for_unread)
              {
                reader.checkForUnread ();
              }
            return decoder;
          }
        catch (InstantiationException e)
          {
            throw new IOException (e);
          }
        catch (IllegalAccessException e)
          {
            throw new IOException (e);
          }
      }

    public JSONDecoder parse (byte[] json_utf8) throws IOException
      {
        return parse(JSONParser.parse (json_utf8));
      }

    public void addToCache (Class<? extends JSONDecoder> json_decoder) throws IOException
      {
        try
          {
            JSONDecoder decoder = json_decoder.newInstance ();
            String object_type_identifier = decoder.getContext ();
            if (decoder.getQualifier () != null)
              {
                object_type_identifier += CONTEXT_QUALIFIER_DIVIDER + decoder.getQualifier ();
              }
            if (class_map.put (object_type_identifier, decoder.getClass ()) != null)
              {
                throw new IOException ("JSON document type already defined: " + object_type_identifier);
              }
          }
        catch (InstantiationException ie)
          {
            throw new IOException ("Class " + json_decoder.getName () + " is not a valid JSONDecoder", ie);
          }
        catch (IllegalAccessException iae)
          {
            throw new IOException ("Class " + json_decoder.getName () + " is not a valid JSONDecoder", iae);
          }
      }

    public void addToCache (String json_decoder_path) throws IOException
      {
        try
          {
            addToCache (Class.forName (json_decoder_path).asSubclass (JSONDecoder.class));
          }
        catch (ClassNotFoundException cnfe)
          {
            throw new IOException ("Class " + json_decoder_path + " can't be found", cnfe);
          }
      }

    public void setCheckForUnreadProperties (boolean flag)
      {
        check_for_unread = flag;
      }
  }

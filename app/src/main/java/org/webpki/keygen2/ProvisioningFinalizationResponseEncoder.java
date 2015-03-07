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
package org.webpki.keygen2;

import java.io.IOException;

import org.webpki.json.JSONEncoder;
import org.webpki.json.JSONObjectWriter;

import static org.webpki.keygen2.KeyGen2Constants.*;


public class ProvisioningFinalizationResponseEncoder extends JSONEncoder
  {
    private static final long serialVersionUID = 1L;

    String client_session_id;

    String server_session_id;

    byte[] attestation;

    // Constructors

    public ProvisioningFinalizationResponseEncoder (ProvisioningFinalizationRequestDecoder fin_prov_request, byte[] attestation)
      {
        client_session_id = fin_prov_request.getClientSessionId ();
        server_session_id = fin_prov_request.getServerSessionId ();
        this.attestation = attestation;
      }

    @Override
    protected void writeJSONData (JSONObjectWriter wr) throws IOException
      {
        //////////////////////////////////////////////////////////////////////////
        // Session properties
        //////////////////////////////////////////////////////////////////////////
        wr.setString (SERVER_SESSION_ID_JSON, server_session_id);

        wr.setString (CLIENT_SESSION_ID_JSON, client_session_id);

        wr.setBinary (ATTESTATION_JSON, attestation);
      }

    @Override
    public String getQualifier ()
      {
        return KeyGen2Messages.PROVISIONING_FINALIZATION_RESPONSE.getName ();
      }

    @Override
    public String getContext ()
      {
        return KEYGEN2_NS;
      }
  }

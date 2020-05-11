/*
 *  Copyright 2015-2020 WebPKI.org (http://webpki.org).
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
package org.webpki.mobile.android.keygen2;

import android.os.AsyncTask;

import android.widget.Button;
import android.widget.TextView;

import android.view.View;

import org.webpki.mobile.android.R;

import org.webpki.keygen2.CredentialDiscoveryRequestDecoder;
import org.webpki.keygen2.KeyCreationRequestDecoder;
import org.webpki.keygen2.InvocationRequestDecoder;
import org.webpki.keygen2.ProvisioningFinalizationRequestDecoder;
import org.webpki.keygen2.ProvisioningInitializationRequestDecoder;

public class KeyGen2ProtocolInit extends AsyncTask<Void, String, Boolean> {
    private KeyGen2Activity keyGen2Activity;

    public KeyGen2ProtocolInit(KeyGen2Activity keygen2_activity) {
        this.keyGen2Activity = keygen2_activity;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            keyGen2Activity.getProtocolInvocationData();
            keyGen2Activity.addDecoder(InvocationRequestDecoder.class);
            keyGen2Activity.addDecoder(ProvisioningInitializationRequestDecoder.class);
            keyGen2Activity.addDecoder(KeyCreationRequestDecoder.class);
            keyGen2Activity.addDecoder(CredentialDiscoveryRequestDecoder.class);
            keyGen2Activity.addDecoder(ProvisioningFinalizationRequestDecoder.class);
            keyGen2Activity.invocationRequest = (InvocationRequestDecoder) keyGen2Activity.getInitialRequest();
            return true;
        } catch (Exception e) {
            keyGen2Activity.logException(e);
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (keyGen2Activity.userHasAborted()) {
            return;
        }
        keyGen2Activity.noMoreWorkToDo();
        if (success) {
            ((TextView) keyGen2Activity.findViewById(R.id.partyInfo)).setText(keyGen2Activity.getRequestingHost());
            keyGen2Activity.findViewById(R.id.primaryWindow).setVisibility(View.VISIBLE);
            final Button ok = (Button) keyGen2Activity.findViewById(R.id.OKbutton);
            final Button cancel = (Button) keyGen2Activity.findViewById(R.id.cancelButton);
            ok.requestFocus();
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    keyGen2Activity.findViewById(R.id.primaryWindow).setVisibility(View.INVISIBLE);
                    keyGen2Activity.logOK("The user hit OK");
                    new KeyGen2SessionCreation(keyGen2Activity).execute();
                }
            });
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    keyGen2Activity.conditionalAbort(null);
                }
            });
        } else {
            keyGen2Activity.showFailLog();
        }
    }
}

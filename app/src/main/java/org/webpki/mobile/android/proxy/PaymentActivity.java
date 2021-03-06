/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
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
package org.webpki.mobile.android.proxy;

import android.app.Activity;

import android.content.Intent;

import android.net.Uri;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;

import android.util.Log;

import org.webpki.mobile.android.R;

import java.io.ByteArrayInputStream;

import java.security.GeneralSecurityException;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.util.ArrayList;

public class PaymentActivity extends Activity {

    private Handler mWaitHandler = new Handler();

    private void bad(String message) {
        Log.e("EEE", message);
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            bad("Missing extras");
            return;
        }
        ArrayList<String> methodNames = extras.getStringArrayList("methodNames");
        if (methodNames == null || methodNames.size() != 1) {
            bad("Missing or too many methodNames");
            return;
        }
        Bundle methodData = extras.getBundle("methodData");
        if (methodData == null) {
            bad("Missing methodData");
            return;
        }
        String jsonString = methodData.getString(methodNames.get(0));
        if (jsonString == null) {
            bad("Missing 'data'");
            return;
        }
        // ["the url we are looking for"]
        final Uri proxyUrl = Uri.parse(jsonString.substring(2, jsonString.length() - 2));
        String topLevelOrigin = extras.getString("topLevelOrigin");
        if (topLevelOrigin == null ||
            !topLevelOrigin.equals(extras.getString("paymentRequestOrigin"))) {
            bad("iframe call not allowed");
            return;
        }
        int i = topLevelOrigin.indexOf(':');
        if (i > 0) {
            topLevelOrigin = topLevelOrigin.substring(0, i);
        }
        if (!topLevelOrigin.equals(Uri.parse(proxyUrl.getQueryParameter("url")).getHost())) {
            bad("url host mismatch");
            return;
        }
        Parcelable[] topLevelCertChain = extras.getParcelableArray("topLevelCertificateChain");
        if (topLevelCertChain == null) {
            bad("missing certChain");
            return;
        }
        final byte[] eeCert = ((Bundle)topLevelCertChain[0]).getByteArray("certificate");
        if (eeCert == null) {
            bad("missing eeCert");
            return;
        }
        final Class<? extends BaseProxyActivity> executor = BaseProxyActivity.getExecutor(proxyUrl);
        mWaitHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), executor);
                intent.setData(proxyUrl);
                intent.putExtra("eeCert", eeCert);
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Remove all the callbacks otherwise navigation will execute even after activity is killed or closed.
        mWaitHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                Log.i("FFF", "OK");
                setResult(Activity.RESULT_OK, data);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i("FFF", "Cancel");
                //Write your code if there's no result
            }
            finish();
        }
    }
}

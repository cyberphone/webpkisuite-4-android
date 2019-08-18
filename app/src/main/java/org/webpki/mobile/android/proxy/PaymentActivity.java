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
        String tempUrl = methodData.getString(methodNames.get(0));
        if (tempUrl == null) {
            bad("Missing 'data'");
            return;
        }
        final Uri url = Uri.parse(tempUrl.substring(8, tempUrl.length() - 2));
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
        if (!topLevelOrigin.equals(Uri.parse(url.getQueryParameter("url")).getHost())) {
            bad("url host mismatch");
            return;
        }
        Parcelable[] topLevelCertificateChain = extras.getParcelableArray("topLevelCertificateChain");
        if (topLevelCertificateChain == null) {
            bad("missing certchain");
            return;
        }
        byte[] eeCertificateBytes = ((Bundle)topLevelCertificateChain[0]).getByteArray("certificate");
        if (eeCertificateBytes == null) {
            bad("missing certificate");
            return;
        }
        final X509Certificate eeCertificate;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            eeCertificate = (X509Certificate)
                    cf.generateCertificate(new ByteArrayInputStream(eeCertificateBytes));
        } catch (GeneralSecurityException e) {
            bad(e.getMessage());
            return;
        }
        Log.i("KKK", url.getHost());
        Log.i("KKK", eeCertificate.toString());
        final Class<? extends BaseProxyActivity> executor = BaseProxyActivity.getExecutor(url);
        mWaitHandler.post(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), executor);
                intent.setData(url);
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
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.i("FFF", "Cancel");
                //Write your code if there's no result
            }
            finish();
        }
    }
}

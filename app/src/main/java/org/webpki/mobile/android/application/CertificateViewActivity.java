/*
 *  Copyright 2013-2020 WebPKI.org (http://webpki.org).
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
package org.webpki.mobile.android.application;

import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import android.os.Bundle;

import android.webkit.WebView;

import android.app.Activity;

import android.content.Intent;

import org.webpki.crypto.CertificateInfo;
import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.ExtendedKeyUsages;

import org.webpki.mobile.android.util.WebViewHtmlLoader;

import org.webpki.util.HTMLEncoder;
import org.webpki.util.ArrayUtil;
import org.webpki.util.HexaDecimal;

import org.webpki.mobile.android.R;

public class CertificateViewActivity extends Activity {
    public static final String CERTIFICATE_BLOB = "cert_blob";

    private String niceDate(GregorianCalendar dateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(dateTime.getTime());
    }


    private void add(StringBuilder s, String header, String data) {
        s.append("<tr valign='middle' bgcolor='#e0e0e8'><td>")
         .append(header)
         .append("</td><td><code>")
         .append(data)
         .append("<code></td></tr>");
    }


    private void printURIs(StringBuilder s, String header, String[] inuris) throws IOException {
        if (inuris != null) {
            StringBuilder arg = new StringBuilder();
            boolean break_it = false;
            for (String uri : inuris) {
                if (break_it) {
                    arg.append("<br>");
                } else {
                    break_it = true;
                }
                arg.append(uri);
            }
            add(s, header, arg.toString());
        }
    }


    private String formatCodeString(String hex_with_spaces) {
        StringBuilder dump = new StringBuilder();
        for (char c : hex_with_spaces.toCharArray()) {
            if (c == '\n') {
                dump.append("<br>");
            } else if (c == ' ') {
                dump.append("&nbsp;");
            } else {
                dump.append(c);
            }
        }
        return dump.toString();
    }

    private String binaryDump(byte[] binary, boolean show_text) {
        return formatCodeString(HexaDecimal.getHexDebugData(binary, show_text ? 16 : -16));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cert_view);
        WebView certView = (WebView) findViewById(R.id.certData);
        Intent intent = getIntent();
        StringBuilder certText =
            new StringBuilder("<html><body><table cellspacing='5' cellpadding='5'>");
        try {
            CertificateInfo cert_info =
                new CertificateInfo(CertificateUtil.getCertificateFromBlob(
                    intent.getByteArrayExtra(CERTIFICATE_BLOB)));
            add(certText, "Issuer", HTMLEncoder.encode(cert_info.getIssuer()));
            add(certText, "Serial&nbsp;number", cert_info.getSerialNumber() +
                " (0x" + cert_info.getSerialNumberInHex() + ")");
            add(certText, "Subject", HTMLEncoder.encode(cert_info.getSubject()));
            add(certText, "Valid&nbsp;from", niceDate(cert_info.getNotBeforeDate()));
            add(certText, "Valid&nbsp;to", niceDate(cert_info.getNotAfterDate()));
            String bc = cert_info.getBasicConstraints();
            if (bc != null) {
                add(certText, "Basic&nbsp;constraints", bc);
            }
            printURIs(certText, "Key&nbsp;usage", cert_info.getKeyUsages());
            String[] ext_key_usages = cert_info.getExtendedKeyUsage();
            if (ext_key_usages != null) {
                for (int i = 0; i < ext_key_usages.length; i++) {
                    ext_key_usages[i] =
                        ExtendedKeyUsages.getOptionallyTranslatedEku(ext_key_usages[i]);
                }
                printURIs(certText, "Extended&nbsp;key&nbsp;usage", ext_key_usages);
            }
            printURIs(certText, "Policy&nbsp;OIDs", cert_info.getPolicyOIDs());
            printURIs(certText, "AIA&nbsp;CA&nbsp;issuers", cert_info.getAIACAIssuers());
            printURIs(certText, "OCSP&nbsp;reponders", cert_info.getAIAOCSPResponders());
            String fp = ArrayUtil.toHexString(cert_info.getCertificateHash(), 0, -1, true, ' ');
            add(certText, "SHA256&nbsp;thumbprint",
                fp.substring(0, 47) + "<br>" + fp.substring(47));
            add(certText, "Key&nbsp;algorithm", cert_info.getPublicKeyAlgorithm());
            add(certText, "Public&nbsp;key", binaryDump(cert_info.getPublicKeyData(), false));
            certText.append("</table>");
        } catch (Exception e) {
            certText =
                new StringBuilder("<html><body><font color='red'>FAILED: ").append(e.getMessage());
                for (StackTraceElement ste : e.getStackTrace()) {
                    certText.append("<br>").append(ste.toString());
                }
        }
        WebViewHtmlLoader.loadHtml(certView, certText.append("</body></html>"));
    }
}

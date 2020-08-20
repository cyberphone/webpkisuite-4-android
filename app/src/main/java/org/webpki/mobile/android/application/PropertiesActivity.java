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

import org.webpki.crypto.DeviceID;

import org.webpki.sks.SKSException;

import org.webpki.mobile.android.R;

import org.webpki.mobile.android.keygen2.KeyGen2Activity;

import org.webpki.mobile.android.webauth.WebAuthActivity;

import org.webpki.mobile.android.saturn.SaturnActivity;

import org.webpki.mobile.android.sks.AndroidSKSImplementation;
import org.webpki.mobile.android.sks.HardwareKeyStore;

import android.net.Uri;

import android.os.Bundle;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;

import android.content.DialogInterface;
import android.content.Intent;

public class PropertiesActivity extends ListActivity {

    static final int SETTINGS_ABOUT            = 0;
    static final int SETTINGS_PRIVACY_POLICY   = 1;
    static final int SETTINGS_DEVICE_ID        = 2;
    static final int SETTINGS_USER_CREDENTIALS = 3;
    static final int SETTINGS_DEVICE_CERT      = 4;
    static final int SETTINGS_PROTOCOL_LOG     = 5;
    static final int SETTINGS_THEME            = 6;
    static final int SETTINGS_ACCESSIBILITY    = 7;
    String[] items = {"About",
                      "Privacy Policy",
                      "Device ID",
                      "User Credentials",
                      "Device Certificate",
                      "Show Protocol Log",
                      "UI Theme",
                      "Accessibility"};

    AndroidSKSImplementation sks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_properties);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
        registerForContextMenu(getListView());
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        sks = HardwareKeyStore.createSKS("Dialog", getBaseContext(), true);
        if (id == SETTINGS_DEVICE_CERT) {
            Intent intent = new Intent(this, CertificateViewActivity.class);
            try {
                intent.putExtra(CertificateViewActivity.CERTIFICATE_BLOB,
                                sks.getDeviceInfo().getCertificatePath()[0].getEncoded());
            } catch (Exception e) {
                intent.putExtra(CertificateViewActivity.CERTIFICATE_BLOB, new byte[]{});
            }
            startActivity(intent);
        } else if (id == SETTINGS_PRIVACY_POLICY) {
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                "https://cyberphone.github.io/doc/webpki-android-privacy-policy.html"));
            startActivity(intent);
        } else if (id == SETTINGS_USER_CREDENTIALS) {
            try {
                if (sks.enumerateKeys(0) != null) {
                    Intent intent = new Intent(this, CredentialsActivity.class);
                    startActivity(intent);
                    return;
                }
            } catch (SKSException e) {
            }
            showDialog(position);
        } else if (id == SETTINGS_PROTOCOL_LOG ||
                   id == SETTINGS_THEME ||
                   id == SETTINGS_ACCESSIBILITY) {
            super.onListItemClick(l, v, position, id);
            v.showContextMenu();
        } else {
            showDialog(position);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        switch (((AdapterView.AdapterContextMenuInfo) menuInfo).position) {
            case SETTINGS_PROTOCOL_LOG:
                menu.setHeaderTitle("Show last run with:");
                menu.add(KeyGen2Activity.KEYGEN2);
                menu.add(WebAuthActivity.WEBAUTH);
                menu.add(SaturnActivity.SATURN);
                break;
            case SETTINGS_THEME:
                menu.setHeaderTitle("Set UI Theme:");
                boolean whiteTheme = Settings.isWhiteTheme();
                menu.add(1, 0, Menu.NONE, "Space").setChecked(!whiteTheme);
                menu.add(1, 1, Menu.NONE, "White").setChecked(whiteTheme);
                menu.setGroupCheckable(1, true, true);
                break;
            case SETTINGS_ACCESSIBILITY:
                menu.setHeaderTitle("Accessibility:");
                boolean visuallyImpaired = Settings.isVisuallyImpaired();
                menu.add(2, 0, Menu.NONE, "Normal").setChecked(!visuallyImpaired);
                menu.add(2, 1, Menu.NONE, "Visually Impaired").setChecked(visuallyImpaired);
                menu.setGroupCheckable(2, true, true);
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == 1) {
            Settings.writeTheme(item.getItemId() == 1);
        } else if (item.getGroupId() == 2) {
            Settings.writeAccessibility(item.getItemId() == 1);
        } else {
            Intent intent = new Intent(this, ProtocolViewActivity.class);
            intent.putExtra(ProtocolViewActivity.LOG_FILE, item.getTitle());
            startActivity(intent);
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case SETTINGS_ABOUT:
                AlertDialog.Builder about_builder = new AlertDialog.Builder(this);
                about_builder.setTitle("About");
                String version = "??";
                try {
                    version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (Exception e) {
                }
                about_builder.setMessage("This application was developed by PrimeKey " +
                    "Solutions and WebPKI.org\n\nCurrent version: " + version);
                about_builder.setIcon(android.R.drawable.btn_star_big_on);
                about_builder.setPositiveButton(android.R.string.ok,
                                                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                return about_builder.create();

            case SETTINGS_DEVICE_ID:
                AlertDialog.Builder device_id_builder = new AlertDialog.Builder(this);
                device_id_builder.setTitle("Device ID");
                device_id_builder.setIcon(android.R.drawable.ic_menu_info_details);
                try {
                    StringBuilder devid = new StringBuilder(DeviceID.getDeviceId(
                            sks.getDeviceInfo().getCertificatePath()[0], false));
                    for (int i = 0, j = 4; i < 4; i++, j += 5) {
                        devid.insert(j, '-');
                    }
                    device_id_builder.setMessage(devid);
                } catch (SKSException e) {
                    device_id_builder.setMessage("Something went wrong");
                }
                device_id_builder.setPositiveButton(android.R.string.ok,
                                                    new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                return device_id_builder.create();

            case SETTINGS_USER_CREDENTIALS:
                AlertDialog.Builder no_credentials_alert = new AlertDialog.Builder(this);
                no_credentials_alert.setTitle("User Credentials");
                no_credentials_alert.setIcon(android.R.drawable.ic_menu_info_details);
                no_credentials_alert.setMessage("You have no credentials yet");
                no_credentials_alert.setPositiveButton(android.R.string.ok,
                                                       new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                return no_credentials_alert.create();

            default:
                Toast.makeText(getApplicationContext(),
                               "Not implemented!",
                               Toast.LENGTH_SHORT).show();
        }
        return null;
    }
}

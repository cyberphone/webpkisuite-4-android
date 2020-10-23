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
package org.webpki.mobile.android.receipts;

import android.content.ContentValues;
import android.content.Context;

import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.provider.BaseColumns;

import java.util.Date;

public class Database extends SQLiteOpenHelper {

    static final int DATABASE_VERSION = 5;
    static final String DATABASE_NAME = "Saturn.db";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_RECEIPTS_TABLE);
        db.execSQL(SQL_CREATE_LOGOTYPES_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_RECEIPTS_TABLE);
        db.execSQL(SQL_DELETE_LOGOTYPES_TABLE);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private static final String SQL_CREATE_RECEIPTS_TABLE =
        "CREATE TABLE " + ReceiptsEntry.RECEIPTS_TABLE + " (" +
            ReceiptsEntry._ID +              " TEXT PRIMARY KEY," + // Receipt URL
            // 0 = OK, MAX_RETRIES = Permanently failed
            ReceiptsEntry.STATUS +           " INTEGER," +
            // Reused as try time for receipts that haven't yet been properly received
            ReceiptsEntry.PAYEE_TIME_STAMP + " INTEGER," +
            ReceiptsEntry.COMMON_NAME +      " TEXT," +
            ReceiptsEntry.AMOUNT +           " TEXT," +
            ReceiptsEntry.CURRENCY +         " TEXT," +
            ReceiptsEntry.JSON_RECEIPT +     " BLOB," +
            ReceiptsEntry.LOGOTYPE_HASH +    " BLOB)";

    private static final String SQL_DELETE_RECEIPTS_TABLE =
        "DROP TABLE IF EXISTS " + ReceiptsEntry.RECEIPTS_TABLE;

    public static class ReceiptsEntry implements BaseColumns {
        public static final String RECEIPTS_TABLE   = "RECEIPTS";
        public static final String STATUS           = "status";
        public static final String PAYEE_TIME_STAMP = "payeeTimeStamp";
        public static final String COMMON_NAME      = "commonName";
        public static final String AMOUNT           = "amount";
        public static final String CURRENCY         = "currency";
        public static final String JSON_RECEIPT     = "jsonReceipt";
        public static final String LOGOTYPE_HASH    = "logotypeHash";
    }

    private static final String SQL_CREATE_LOGOTYPES_TABLE =
        "CREATE TABLE " + LogotypesEntry.LOGOTYPES_TABLE + " (" +
            LogotypesEntry._ID +        " BLOB PRIMARY KEY," + // Logotype SHA256
            LogotypesEntry.IMAGE_DATA + " BLOB," +
            LogotypesEntry.MIME_TYPE +  " TEXT)";

    private static final String SQL_DELETE_LOGOTYPES_TABLE =
        "DROP TABLE IF EXISTS " + LogotypesEntry.LOGOTYPES_TABLE;

    public static class LogotypesEntry implements BaseColumns {
        public static final String LOGOTYPES_TABLE = "LOGOTYPES";
        public static final String IMAGE_DATA      = "imageData";
        public static final String MIME_TYPE       = "mimeType";
    }

    static SQLiteDatabase instance;

    public static SQLiteDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new Database(context).getWritableDatabase();
        }
        return instance;
    }
    public static void AddReceipt(ReceiptData receiptData, Context context)  {
        SQLiteDatabase db = getInstance(context);

        ContentValues values = new ContentValues();
        values.put(ReceiptsEntry._ID, receiptData.receiptUrl);
        values.put(ReceiptsEntry.STATUS, 0);
        values.put(ReceiptsEntry.COMMON_NAME,receiptData.commonName);
        values.put(ReceiptsEntry.AMOUNT, receiptData.amount);
        values.put(ReceiptsEntry.CURRENCY, receiptData.currency);
        values.put(ReceiptsEntry.PAYEE_TIME_STAMP, receiptData.payeeTimeStamp);
        values.put(ReceiptsEntry.JSON_RECEIPT, receiptData.jsonRreceipt);
        values.put(ReceiptsEntry.LOGOTYPE_HASH, receiptData.logotypeHash);
        db.insertWithOnConflict(ReceiptsEntry.RECEIPTS_TABLE, null, values,
                                SQLiteDatabase.CONFLICT_REPLACE);

        values = new ContentValues();
        values.put(LogotypesEntry._ID, receiptData.logotypeHash);
        values.put(LogotypesEntry.IMAGE_DATA, receiptData.logotype);
        values.put(LogotypesEntry.MIME_TYPE, receiptData.mimeType);
        db.insertWithOnConflict(LogotypesEntry.LOGOTYPES_TABLE, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static Cursor getReceiptSelection(Context context) {
        SQLiteDatabase db = getInstance(context);
        return db.rawQuery("SELECT " + ReceiptsEntry._ID + "," +
                                "date(" + ReceiptsEntry.PAYEE_TIME_STAMP +
                                      ",'unixepoch','localtime')," +
                                ReceiptsEntry.COMMON_NAME + "," +
                                ReceiptsEntry.AMOUNT + "||' '||" +
                                ReceiptsEntry.CURRENCY +
                                " FROM " + ReceiptsEntry.RECEIPTS_TABLE,
                           new String[0]);
    }
}

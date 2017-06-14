package com.red.redbluetooth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

/**
 * Created by Red on 2017/5/8.
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {
    public static final String CREATE_DATA_SOURCE = "create table DataSource ("
            + "id integer primary key autoincrement,"
            + "get_or_send integer,"
            + "content text,"
            + "length integer,"
            + "groupNum integer)";

    private Context mContext;

    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DATA_SOURCE);
        Toast.makeText(mContext, "success to create!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
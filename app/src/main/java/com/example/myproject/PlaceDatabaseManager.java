package com.example.myproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

//데이터베이스 파일.
public class PlaceDatabaseManager {
    static final String DB_PLACE = "favorite_place_.db";   //DB이름
    static final String TABLE_PLACE = "fa_pl_"; //Table 이름
    static final int DB_VERSION = 1;			//DB 버전

    Context myContext = null;

    private static PlaceDatabaseManager myDBManager = null;
    private SQLiteDatabase mydatabase = null;

    //PlaceDatabaseManager 싱글톤 패턴으로 구현
    public static PlaceDatabaseManager getInstance(Context context)
    {
        if(myDBManager == null)
        {
            myDBManager = new PlaceDatabaseManager(context);
        }

        return myDBManager;
    }
    public Cursor query(String[] colums,
                        String selection,
                        String[] selectionArgs,
                        String groupBy,
                        String having,
                        String orderby)
    {
        return mydatabase.query(TABLE_PLACE,
                colums,
                selection,
                selectionArgs,
                groupBy,
                having,
                orderby);
    }
    private PlaceDatabaseManager(Context context) //데이터베이스 생성
    {
        myContext = context;

        //DB Open
        mydatabase = context.openOrCreateDatabase(DB_PLACE, context.MODE_PRIVATE,null);

        //Table 생성
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PLACE +
                "(" + "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "NAME TEXT," +
                "LATITUDE TEXT," +
                "LONGITUDE TEXT" +
                ");");
    }
    public long insert(ContentValues addRowValue){
        return mydatabase.insert(TABLE_PLACE, null, addRowValue);
    }
    public int delete(String whereClause,
                      String[] whereArgs)
    {
        return mydatabase.delete(TABLE_PLACE,
                whereClause,
                whereArgs);
    }
}

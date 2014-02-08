package com.qweex.eyebrows;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.os.Bundle;

public class SavedServers
{
    /** The file containing the databases. */
    private static final String DATABASE= "Eyebrows.db";
    /** One of the tables in the SQL database. */
    private static final String DATABASE_TABLE = "sites";
    /** The database for the app. */
    private static SQLiteDatabase database;
    /** A tool to help with the opening of the database. It's in the Android doc examples, yo.*/
    private static DatabaseOpenHelper databaseOpenHelper;

    /** needs to be called once
     * @param context The context to associate with the connector.
     * */
    public static void initialize(Context context)
    {
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE, null, 1);
    }

    public static String[] getAllNames() {
        open();
        Cursor c = database.query(DATABASE_TABLE, new String[]{"_id", "host", "port", "ssl", "username", "password"},
                null, null, null, null, null);
        String[] res = new String[c.getCount()];
        for(int i=0; i<c.getCount(); i++)
            res[i] = c.getString(c.getColumnIndex("name"));
        close();
        return res;
    }

    public static Bundle[] getAll() {
        open();
        Cursor c = database.query(DATABASE_TABLE, new String[]{"_id", "host", "port", "ssl", "username", "password"},
                null, null, null, null, null);
        Bundle[] bundle = new Bundle[c.getCount()];
        for(int i=0; i<c.getCount(); i++)
        {
            Bundle b = new Bundle();
            b.putString("name", c.getString(c.getColumnIndex("name")));
            b.putString("host", c.getString(c.getColumnIndex("host")));
            b.putInt("port", c.getInt(c.getColumnIndex("port")));
            b.putBoolean("ssl", c.getInt(c.getColumnIndex("host"))>0);
            b.putString("username", c.getString(c.getColumnIndex("username")));
            b.putString("password", c.getString(c.getColumnIndex("password")));
            bundle[i] = b;
        }
        close();
        return bundle;
    }

    public static Bundle get(String name) {
        open();
        Cursor c = database.query(DATABASE_TABLE, new String[]{"_id", "host", "port", "ssl", "username", "password"},
                "name='" + sanitize(name) + "'", null, null, null, null);
        c.moveToFirst();
        Bundle b = new Bundle();
        b.putString("host", c.getString(c.getColumnIndex("host")));
        b.putInt("port", c.getInt(c.getColumnIndex("port")));
        b.putBoolean("ssl", c.getInt(c.getColumnIndex("host"))>0);
        b.putString("username", c.getString(c.getColumnIndex("username")));
        b.putString("password", c.getString(c.getColumnIndex("password")));
        close();
        return b;
    }

    public static void remove(String name) {
        open();
        database.execSQL("DELETE FROM " + DATABASE_TABLE + " WHERE name='" + sanitize(name) + "';");
        close();
    }

    public static void add(Bundle values) {
        open();
        ContentValues newFav = new ContentValues();
        newFav.put("name", values.getString("name"));
        newFav.put("port", values.getInt("port"));
        newFav.put("ssl", values.getBoolean("ssl") ? 1 : 0);
        newFav.put("username", values.getString("username"));
        newFav.put("password", values.getString("password"));

        database.insert(DATABASE_TABLE, null, newFav);
        close();
    }


    /** Opens the database so that it can be read or written. */
    private static void open() throws SQLException
    {
        database = databaseOpenHelper.getWritableDatabase();
        databaseOpenHelper.onUpgrade(database, 0, 1);
    }

    /** Closes the database when you are done with it. */
    private static void close()
    {
        if (database != null)
            database.close();
    }




    /** Helper open class for DatabaseConnector */
    private static class DatabaseOpenHelper extends SQLiteOpenHelper
    {
        public DatabaseOpenHelper(Context context, String name, CursorFactory factory, int version)
        {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            String createQuery = "CREATE TABLE " + DATABASE_TABLE + " " +
                    "(_id integer primary key autoincrement" +
                    ", name TEXT unique" +
                    ", host TEXT" +
                    ", port INTEGER" +
                    ", ssl INTEGER" +
                    ", username TEXT" +
                    ", password TEXT" +
                    ");";
            db.execSQL(createQuery);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
        }
    }

    private static String sanitize(String in)
    {
        return in.replaceAll("'", "\\'");
    }
}
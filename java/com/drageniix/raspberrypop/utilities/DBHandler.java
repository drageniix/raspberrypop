package com.drageniix.raspberrypop.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.activities.CreationActivity;
import com.drageniix.raspberrypop.activities.MainActivity;
import com.drageniix.raspberrypop.activities.ScanActivity;
import com.drageniix.raspberrypop.activities.SplashScreenActivity;
import com.drageniix.raspberrypop.dialog.Event;
import com.drageniix.raspberrypop.dialog.Form;
import com.drageniix.raspberrypop.media.Media;
import com.drageniix.raspberrypop.dialog.adapter.cycle.CycleManager;
import com.drageniix.raspberrypop.servers.ServerBase;
import com.drageniix.raspberrypop.servers.kodi_servers.KodiServer;
import com.drageniix.raspberrypop.servers.plex_servers.PlexServer;
import com.drageniix.raspberrypop.services.AlarmReceiver;
import com.drageniix.raspberrypop.utilities.api.APIParser;
import com.drageniix.raspberrypop.utilities.api.ThumbnailAPI;
import com.drageniix.raspberrypop.utilities.billing.Billing;
import com.drageniix.raspberrypop.utilities.categories.AuxiliaryApplication;
import com.drageniix.raspberrypop.utilities.categories.ScanApplication;
import com.drageniix.raspberrypop.utilities.categories.StreamingApplication;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.drageniix.raspberrypop.utilities.categories.StreamingApplication.*;

public class DBHandler extends SQLiteOpenHelper {
    private static DBHandler sInstance;
    private static boolean initialized;

    private BaseActivity context;
    private APIParser parser;
    private Preferences preferences;
    private FormTemplates templates;
    private FileHelper fileHelper;
    private ServerCollection servers;
    private MediaCollection mediaList;
    private String defaultCollection;
    private Billing billing;
    private TimeManager timeManager;
    private CycleManager cycleManager;

    private static final int DATABASE_VERSION = 10;
    private static final String DATABASE_NAME = "RaspberryPOP";

    private static final String TABLE_SERVER = "plex";
    private static final String SERVER_COLUMN_KEY = "_key";
    private static final String SERVER_COLUMN_NAME = "name";
    private static final String SERVER_COLUMN_SERVERID = "serverid";
    private static final String SERVER_COLUMN_HOST = "host";
    private static final String SERVER_COLUMN_PORT = "port";
    private static final String SERVER_COLUMN_AUTH = "auth";
    private static final String SERVER_COLUMN_LIBRARIES = "libraries";
    private static final String SERVER_COLUMN_CLIENT = "client";
    private static final String SERVER_COLUMN_TYPE = "type";

    private static final String TABLE_MEDIA = "media";
    private static final String MEDIA_COLUMN_KEY = "_key";
    private static final String MEDIA_COLUMN_NFCID = "nfcid";
    private static final String MEDIA_COLUMN_ORIGINAL = "original";
    private static final String MEDIA_COLUMN_COMMENTS = "comments";
    private static final String MEDIA_COLUMN_FIGURE = "figure";
    private static final String MEDIA_COLUMN_POSITION = "position";
    private static final String MEDIA_COLUMN_CYCLE = "cycle";
    private static final String MEDIA_COLUMN_SEQ = "sequential";
    private static final String MEDIA_COLUMN_CATEGORY = "category";
    private static final String MEDIA_COLUMN_LABEL = "label";
    private static final String MEDIA_COLUMN_ENABLED = "enabled";
    private static final String MEDIA_COLUMN_STREAMING = "streaming";
    private static final String MEDIA_COLUMN_ALTERNATE = "alternate";
    private static final String MEDIA_COLUMN_TYPE = "type";
    private static final String MEDIA_COLUMN_TITLE = "title";
    private static final String MEDIA_COLUMN_DETAIL = "year";
    private static final String MEDIA_COLUMN_SUMMARY = "summary";
    private static final String MEDIA_COLUMN_IMDB = "imdb";
    private static final String MEDIA_COLUMN_THUMBNAIL = "thumbnail";
    private static final String MEDIA_COLUMN_AUXILIARY = "auxiliary";
    private static final String MEDIA_COLUMN_TASKER_ENABLED = "tasker_enabled";
    private static final String MEDIA_COLUMN_TASKER_A = "taskera";
    private static final String MEDIA_COLUMN_TASKER_B = "taskerb";
    private static final String MEDIA_COLUMN_TASKER_PARAMS = "tasker_params";

    private DBHandler(BaseActivity activity) {
        super(activity.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        defaultCollection = activity.getString(R.string.default_collection);
        preferences = new Preferences(activity.getApplicationContext());
        timeManager = new TimeManager(activity);
        Logger.setHandler(this);
        mediaList = new MediaCollection(this, defaultCollection);
        servers = new ServerCollection();
        templates = new FormTemplates(activity.getApplicationContext());
        cycleManager = new CycleManager(this);
        fileHelper = new FileHelper(activity.getApplicationContext(), this);
        parser = new APIParser(this, activity);
        billing = new Billing(activity, this);
        initialized = false;
    }

    public DBHandler(Context context){
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        preferences = new Preferences(context.getApplicationContext());
        timeManager = new TimeManager(context);
        Logger.setHandler(this);

        SQLiteDatabase db = this.getWritableDatabase();
        try(Cursor cursor = db.rawQuery("SELECT " +
                MEDIA_COLUMN_KEY + ", " +
                MEDIA_COLUMN_TITLE + ", " +
                MEDIA_COLUMN_STREAMING +
                " FROM " + TABLE_MEDIA + " WHERE " +
                    MEDIA_COLUMN_TYPE + "='" + AuxiliaryApplication.SCAN_ALARM.name() + "' AND " +
                        MEDIA_COLUMN_DETAIL + " LIKE '% Active%'"
                , null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(MEDIA_COLUMN_KEY));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TITLE)),
                            streamingID = cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_STREAMING));

                    AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    Intent alarmIntent = new Intent(context, AlarmReceiver.class)
                            .putExtra(ScanActivity.UID, id)
                            .putExtra(AlarmReceiver.TITLE, title + " (" + streamingID + " Alarm)")
                            .putExtra(AlarmReceiver.CONTENT, "Scan Tag to Stop Alarm");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), id, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | Intent.FILL_IN_DATA);

                    manager.cancel(pendingIntent);
                    manager.set(AlarmManager.RTC_WAKEUP, Event.parseTime(this, streamingID).getTimeInMillis(), pendingIntent);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        }
    }

    public static synchronized DBHandler getsInstance(BaseActivity activity) {
        if (sInstance == null) {sInstance = new DBHandler(activity);}
        sInstance.context = activity;
        return sInstance;
    }

    public static void initialize(BaseActivity activity){
        if (!initialized && (activity instanceof SplashScreenActivity || activity instanceof MainActivity)) {
            Form.setPattern(activity);
            for (StreamingApplication category : StreamingApplication.values()) {
                category.setIcon(activity, sInstance);
            }
            sInstance.getAllServers();
            for (AuxiliaryApplication category : AuxiliaryApplication.values()){
                category.setIcon(activity, sInstance);}
            for (ScanApplication category : ScanApplication.values()) {
                category.setIcon(activity, sInstance);}
            sInstance.getAllMedia();
            initialized = true;
        }
    }

    String getDatabasePath(){
        return context.getDatabasePath(DBHandler.DATABASE_NAME).getAbsolutePath();}


    public static boolean isInitialized() {
        return initialized;
    }

    public static int getCollectionSize() {
        return initialized ? sInstance.getMediaList(sInstance.getDefaultCollection()).size() : sInstance.preferences.getCollectionSize();
    }

    //GENERAL "STATIC" HOLDING
    public Billing getBilling() {return billing;}
    public APIParser getParser(){return parser;}
    public Preferences getPreferences() {return preferences;}
    public FormTemplates getTemplates() {return templates;}
    public FileHelper getFileHelper(){return fileHelper;}
    public TimeManager getTimeManager() {return timeManager;}
    public CycleManager getCycleManager() {return cycleManager;}

    public Set<PlexServer> getPlexServers(){return servers.getPlexServers();}
    public Set<KodiServer> getKodiServers(){return servers.getKodiServers();}
    public Set<ServerBase> getServers(){return servers;}
    public int getSeversSize(){return servers.size();}

    public String getDefaultCollection(){return defaultCollection;}
    public Set<String> getCollections(){return new LinkedHashSet<>(mediaList.keySet());}
    public Set<Media> getMediaList(String collection) {return mediaList.get(collection);}

    private void updateMenu(){if (context instanceof MainActivity) ((MainActivity)context).updateMenu();}

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_SERVER_TABLE = "CREATE TABLE " + TABLE_SERVER + " (" +
                SERVER_COLUMN_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT DEFAULT 1," +
                SERVER_COLUMN_NAME + " TEXT," +
                SERVER_COLUMN_SERVERID + " TEXT," +
                SERVER_COLUMN_HOST + " TEXT," +
                SERVER_COLUMN_PORT + " TEXT," +
                SERVER_COLUMN_AUTH + " TEXT," +
                SERVER_COLUMN_LIBRARIES + " TEXT," +
                SERVER_COLUMN_CLIENT + " TEXT, " +
                SERVER_COLUMN_TYPE + " TEXT)";

        String CREATE_MEDIA_TABLE = "CREATE TABLE " + TABLE_MEDIA + " (" +
                MEDIA_COLUMN_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT DEFAULT 1," +
                MEDIA_COLUMN_NFCID + " TEXT," +
                MEDIA_COLUMN_ORIGINAL + " TEXT," +
                MEDIA_COLUMN_COMMENTS + " TEXT," +
                MEDIA_COLUMN_FIGURE + " TEXT," +
                MEDIA_COLUMN_POSITION + " INTEGER DEFAULT -1," +
                MEDIA_COLUMN_CYCLE + " TEXT DEFAULT ''," +
                MEDIA_COLUMN_SEQ + " INTEGER DEFAULT 1," +
                MEDIA_COLUMN_CATEGORY + " TEXT," +
                MEDIA_COLUMN_LABEL + " TEXT," +
                MEDIA_COLUMN_ENABLED + " TEXT," +
                MEDIA_COLUMN_STREAMING + " TEXT," +
                MEDIA_COLUMN_ALTERNATE + " TEXT," +
                MEDIA_COLUMN_TYPE + " TEXT," +
                MEDIA_COLUMN_TITLE + " TEXT," +
                MEDIA_COLUMN_DETAIL + " TEXT," +
                MEDIA_COLUMN_SUMMARY + " TEXT," +
                MEDIA_COLUMN_IMDB + " TEXT," +
                MEDIA_COLUMN_THUMBNAIL + " TEXT," +
                MEDIA_COLUMN_AUXILIARY + " TEXT," +
                MEDIA_COLUMN_TASKER_ENABLED + " TEXT," +
                MEDIA_COLUMN_TASKER_A + " TEXT," +
                MEDIA_COLUMN_TASKER_B + " TEXT," +
                MEDIA_COLUMN_TASKER_PARAMS + " TEXT)";

        db.execSQL(CREATE_SERVER_TABLE);
        db.execSQL(CREATE_MEDIA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.beginTransaction();
            if (oldVersion < 10){
                db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + MEDIA_COLUMN_LABEL + " TEXT DEFAULT ''");
            }
            if (oldVersion < 9) {
                db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + MEDIA_COLUMN_COMMENTS + " TEXT DEFAULT ''");
            }
            if (oldVersion < 8) {
                db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + MEDIA_COLUMN_ORIGINAL + " TEXT DEFAULT ''");
            }
            if (oldVersion < 7) {
                db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + MEDIA_COLUMN_SEQ + " INTEGER DEFAULT 1");
            }
            if (oldVersion < 6) {
                db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + MEDIA_COLUMN_CYCLE + " TEXT DEFAULT ''");
            }
            if (oldVersion < 4) {
                preferences.setPurchasedPremium();
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + TABLE_MEDIA + " ADD COLUMN " + MEDIA_COLUMN_POSITION + " INTEGER DEFAULT -1");
                db.delete(TABLE_SERVER, SERVER_COLUMN_TYPE + " =?", new String[]{KODI.name()});
            }
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE " + TABLE_SERVER + " ADD COLUMN " + SERVER_COLUMN_TYPE + " TEXT DEFAULT 'PLEX'");
            }
            db.setTransactionSuccessful();
        } catch (Exception e){
            Logger.log(Logger.DB, e);
        } finally {
            db.endTransaction();
        }
    }

    public long addOrUpdateServer(ServerBase server) {
        long id = -1;
        SQLiteDatabase db = this.getWritableDatabase();
        if (server.getName() != null && !server.getName().isEmpty()) {
            try {
                db.beginTransaction();
                ContentValues values = new ContentValues();
                values.put(SERVER_COLUMN_NAME, server.getName());
                values.put(SERVER_COLUMN_SERVERID, server.getServerID());
                values.put(SERVER_COLUMN_HOST, server.getServerHost());
                values.put(SERVER_COLUMN_PORT, server.getServerPort());
                values.put(SERVER_COLUMN_AUTH, server.getKey());
                values.put(SERVER_COLUMN_LIBRARIES, server.getChosenLibrariesString());
                values.put(SERVER_COLUMN_CLIENT, server.getClient());
                values.put(SERVER_COLUMN_TYPE, server.getType().name());

                int rows = db.update(TABLE_SERVER, values, SERVER_COLUMN_SERVERID + " = ?", new String[]{server.getServerID()});

                if (rows == 1) {
                    try(Cursor cursor = db.query(TABLE_SERVER, new String[]{SERVER_COLUMN_KEY},
                            SERVER_COLUMN_SERVERID + " = ?", new String[]{server.getServerID()},
                            null, null, null, null)) {
                        if (cursor.moveToFirst()) {
                            id = cursor.getInt(cursor.getColumnIndex(SERVER_COLUMN_KEY));
                            db.setTransactionSuccessful();
                        }
                    }
                } else {
                    id = db.insertOrThrow(TABLE_SERVER, null, values);
                    db.setTransactionSuccessful();
                    servers.add(server);
                }
            } catch (Exception e) {
                Logger.log(Logger.DB, e);
            } finally {
                db.endTransaction();
            }
        } else if (servers.contains(server)) {
            deleteServer(server);
            Toast.makeText(context, "Server not found.", Toast.LENGTH_LONG).show();
        }

        return id;
    }

    public void getAllServers() {
        servers = new ServerCollection();
        SQLiteDatabase db = this.getWritableDatabase();
        try(Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SERVER + " ORDER BY " + SERVER_COLUMN_KEY + " ASC", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    ServerBase server = null;
                    if (cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_TYPE)).equals(PLEX.name())){

                        server = new PlexServer(this,
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_NAME)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_SERVERID)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_HOST)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_PORT)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_AUTH)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_CLIENT)));

                    } else if (cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_TYPE)).equals(KODI.name())){

                        server = new KodiServer(this,
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_NAME)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_HOST)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_PORT)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_AUTH)),
                                cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_LIBRARIES)));

                    }
                    servers.add(server);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        }
    }

    public void deleteServer(ServerBase server) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_SERVER, SERVER_COLUMN_SERVERID + " = ?", new String[]{server.getServerID()});
            for (Media media : mediaList.get(defaultCollection)) {
                if (media.getAlternateID().equals(server.getServerID())) {
                    media.turnOff();
                }
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        } finally {
            db.endTransaction();
        }
        servers.remove(server);
    }

    public ServerBase getServer(String input) {
        ServerBase serverResult = null;
        if (input != null && !input.isEmpty()) {
            if (servers != null && !servers.isEmpty()) {
                for (ServerBase server : servers) {
                    if (input.equals(server.getServerID())) {
                        serverResult = server;
                        break;
                    }
                }
            } else {
                try(Cursor cursor = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_SERVER + " WHERE " + SERVER_COLUMN_SERVERID + " = '?'",
                        new String[]{input})) {
                    if (cursor != null && cursor.moveToFirst()) {
                        if (cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_TYPE)).equals(PLEX.name())){

                            serverResult = new PlexServer(this,
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_NAME)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_SERVERID)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_HOST)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_PORT)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_AUTH)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_CLIENT)));

                        } else if (cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_TYPE)).equals(KODI.name())){

                            serverResult = new KodiServer(this,
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_NAME)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_HOST)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_PORT)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_AUTH)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(SERVER_COLUMN_LIBRARIES)));
                        }
                    }
                } catch (Exception e) {
                    Logger.log(Logger.DB, e);
                }
            }
        }
        return serverResult;
    }

    public void updateMediaPosition(Media media, int position){
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            media.setPosition(position);
            ContentValues values = new ContentValues();
            values.put(MEDIA_COLUMN_POSITION, media.getPosition());
            db.update(TABLE_MEDIA, values, MEDIA_COLUMN_NFCID + " LIKE ?", new String[]{"%" + media.getScanUID() + "%"});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        } finally {
            db.endTransaction();
        }

        getCycleManager().update(media);
    }

    public void addOrUpdateMedia(Media media) {
        if (media.getCollection() != null) {
            SQLiteDatabase db = this.getWritableDatabase();
            long id = 1;
            try {
                db.beginTransaction();
                ContentValues values = new ContentValues();
                values.put(MEDIA_COLUMN_NFCID, media.getNewScanUID());
                values.put(MEDIA_COLUMN_ORIGINAL, media.getOriginal());
                values.put(MEDIA_COLUMN_COMMENTS, media.getComments());
                values.put(MEDIA_COLUMN_LABEL, media.getLabel());
                values.put(MEDIA_COLUMN_FIGURE, media.getFigureName());
                values.put(MEDIA_COLUMN_POSITION, media.getPosition());
                values.put(MEDIA_COLUMN_CYCLE, media.getCycleString() + "#" + media.getCyclePosition() + "#" + media.getScanHistoryString());
                values.put(MEDIA_COLUMN_SEQ, media.getCycleType());
                values.put(MEDIA_COLUMN_CATEGORY, media.getCollection());
                values.put(MEDIA_COLUMN_ENABLED, media.getEnabled().name());
                values.put(MEDIA_COLUMN_STREAMING, media.getStreamingID());
                values.put(MEDIA_COLUMN_ALTERNATE, media.getAlternateID());
                values.put(MEDIA_COLUMN_TYPE, media.getType());
                values.put(MEDIA_COLUMN_TITLE, media.getTitle());
                values.put(MEDIA_COLUMN_DETAIL, media.getDetail());
                values.put(MEDIA_COLUMN_SUMMARY, media.getSummary());
                values.put(MEDIA_COLUMN_IMDB, media.getExternalID());
                values.put(MEDIA_COLUMN_THUMBNAIL, media.getThumbnailString());
                values.put(MEDIA_COLUMN_AUXILIARY, media.getAuxiliaryString());
                values.put(MEDIA_COLUMN_TASKER_ENABLED, String.valueOf(media.useTasker()));
                values.put(MEDIA_COLUMN_TASKER_A, media.getTaskerTaskA());
                values.put(MEDIA_COLUMN_TASKER_B, media.getTaskerTaskB());
                values.put(MEDIA_COLUMN_TASKER_PARAMS, media.getTaskerParamString());

                int rows = db.update(TABLE_MEDIA, values, MEDIA_COLUMN_NFCID + " LIKE ?", new String[]{"%" + media.getScanUID() + "%"});

                media.setScanUID(media.getNewScanUID());

                if (rows == 1) {
                    try (Cursor cursor = db.query(TABLE_MEDIA, new String[]{MEDIA_COLUMN_KEY},
                            MEDIA_COLUMN_NFCID + " LIKE ?", new String[]{"%" + media.getScanUID() + "%"},
                            null, null, null, null)) {
                        if (cursor.moveToFirst()) {
                            id = cursor.getInt(cursor.getColumnIndex(MEDIA_COLUMN_KEY));
                            db.setTransactionSuccessful();
                        }
                    }
                    if (media.getOldCollection() != null && !media.getOldCollection().equals(media.getCollection())) {
                        mediaList.get(media.getOldCollection()).moveTo(media, mediaList.get(media.getCollection()));
                        updateMenu();
                    }
                } else {
                    boolean updateMenu = !mediaList.containsKey(media.getCollection());

                    cycleManager.loadCycle(media);
                    values.put(MEDIA_COLUMN_CYCLE, media.getCycleString() + "#" + media.getCyclePosition() + "#" + media.getScanHistoryString());
                    values.put(MEDIA_COLUMN_SEQ, media.getCycleType());

                    mediaList.get(media.getCollection()).add(media);
                    id = db.insertOrThrow(TABLE_MEDIA, null, values);
                    db.setTransactionSuccessful();
                    if (updateMenu) updateMenu();
                }
            } catch (Exception e) {
                Logger.log(Logger.DB, e);
            } finally {
                db.endTransaction();
            }
            getCycleManager().update(media);
            media.setId(id);
        }

        preferences.setCollectionSize(mediaList.get(defaultCollection).size());

        if (!media.getEnabled().isFolder() || AuxiliaryApplication.valueOf(media) != null) {
            String qr = media.getEnabled().getQR(context, media);
            if (media.isCreateQR() && media.availableToStream() && qr != null && !qr.isEmpty()) {
                media.setCreateQR();
                Intent intent = new Intent(context, CreationActivity.class)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, qr)
                        .putExtra(Intent.EXTRA_TITLE, media.getFigureName())
                        .putExtra(ScanActivity.UID, media.getScanUID())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    public void getAllMedia() {
        mediaList = new MediaCollection(this, defaultCollection);
        SQLiteDatabase db = this.getWritableDatabase();
        try(Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MEDIA + " ORDER BY " + MEDIA_COLUMN_KEY + " ASC", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String collection = getBilling().hasPremium() ?
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_CATEGORY)) : defaultCollection;

                    Media media = new Media(this,
                            cursor.getInt(cursor.getColumnIndex(MEDIA_COLUMN_KEY)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_NFCID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_ORIGINAL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_COMMENTS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_LABEL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_FIGURE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_CYCLE)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_SEQ)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_POSITION)),
                            collection,
                            new String[]{
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_ENABLED)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_STREAMING)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_ALTERNATE))},
                            new String[]{
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TYPE)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TITLE)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_DETAIL)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_SUMMARY)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_IMDB)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_THUMBNAIL)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_AUXILIARY))},
                            new String[]{
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_ENABLED)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_A)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_B)),
                                    cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_PARAMS))});

                    getCycleManager().loadCycle(media);
                    mediaList.get(collection).add(media);

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        }
    }

    public void deleteMedia(Media media) {
        mediaList.get(media.getCollection()).remove(media);
        if (!media.getCollection().equals(defaultCollection)){
            mediaList.get(defaultCollection).remove(media); }
        if (AuxiliaryApplication.valueOf(media) == AuxiliaryApplication.SCAN_ALARM
                && media.getDetail().equals("Active")) {
            AuxiliaryApplication.SCAN_ALARM.scan(context, this, media, null); }

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_MEDIA, MEDIA_COLUMN_NFCID + " LIKE ?", new String[]{"%" + media.getScanUID() + "%"});
            db.setTransactionSuccessful();
            for(ThumbnailAPI.Type file : ThumbnailAPI.Type.values()){file.remove(media);}
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        } finally {
            db.endTransaction();
        }
        preferences.setCollectionSize(mediaList.get(defaultCollection).size());
    }

    public Media readMedia(String uid) {
        if (uid == null) return null;

        if (mediaList != null && !mediaList.isEmpty()) {
            for (Media media : mediaList.get(defaultCollection)) {
                if (media.getScanUID().contains(uid)) {
                    return media;
                }
            }
        }

        try(Cursor cursor = getWritableDatabase().rawQuery("SELECT " +
                MEDIA_COLUMN_ENABLED + ", " +
                MEDIA_COLUMN_NFCID + ", " +
                MEDIA_COLUMN_CYCLE + ", " +
                MEDIA_COLUMN_SEQ + ", " +
                MEDIA_COLUMN_STREAMING + ", " +
                MEDIA_COLUMN_ALTERNATE + ", " +
                MEDIA_COLUMN_TASKER_ENABLED + ", " +
                MEDIA_COLUMN_TASKER_A + ", " +
                MEDIA_COLUMN_TASKER_B + ", " +
                MEDIA_COLUMN_TASKER_PARAMS +
                " FROM " + TABLE_MEDIA + " WHERE " + MEDIA_COLUMN_NFCID + " LIKE '%" + uid + "%'", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return new Media(this,
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_NFCID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_CYCLE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_SEQ)),
                        new String[]{
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_ENABLED)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_STREAMING)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_ALTERNATE))},
                        new String[]{
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_ENABLED)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_A)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_B)),
                            cursor.getString(cursor.getColumnIndexOrThrow(MEDIA_COLUMN_TASKER_PARAMS))});
            }
        } catch (Exception e) {
            Logger.log(Logger.DB, e);
        }

        return null;
    }
}
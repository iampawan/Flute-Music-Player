package com.mtechviral.musicfinder;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by pawankumar on 22/03/18.
 */

public class MusicFinder {
    private static final String TAG = "MPLAY_MUSIC_FINDER";

    private ContentResolver mContentResolver;
    private List<Song> mSongs = new ArrayList<>();
    private Random mRandom = new Random();
    private HashMap<Long, String> mAlbumMap = new HashMap<>();
    private HashMap<Long, String> mAudioPath= new HashMap<>();

    public MusicFinder(ContentResolver cr) {
        mContentResolver = cr;
    }

    public void prepare() {

        // load all album art
        loadAlbumArt();

        // query all audio path
        loadAudioPath();

        // query all music audio
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cur = mContentResolver.query(uri, null,
                MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);

        if (cur == null) {
            return;
        }
        if (!cur.moveToFirst()) {
            return;
        }

        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumArtColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

        do {
            mSongs.add(new Song(
                    cur.getLong(idColumn),
                    cur.getString(artistColumn),
                    cur.getString(titleColumn),
                    cur.getString(albumColumn),
                    cur.getLong(durationColumn),
                    mAudioPath.get(cur.getLong(idColumn)),
                    mAlbumMap.get(cur.getLong(albumArtColumn))));
        } while (cur.moveToNext());

    }

    private void loadAlbumArt() {
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                System.out.printf("id: %d, path: %s\n", id, path);
                mAlbumMap.put(id, path);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void loadAudioPath() {
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA},
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                mAudioPath.put(id, path);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    public Song getRandomSong() {
        if (mSongs.size() <= 0) return null;
        return mSongs.get(mRandom.nextInt(mSongs.size()));
    }

    public List<Song> getAllSongs() {
        return mSongs;
    }

    public class Song {
        long id;
        String artist;
        String title;
        String album;
        long albumId;
        long duration;
        String uri;
        String albumArt;


        public Song(long id, String artist, String title, String album, long duration, long albumId) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.duration = duration;
            this.albumId = albumId;
            this.uri = getURI();
            this.albumArt = getAlbumArt();
        }

        public Song(long id, String artist, String title, String album, long duration, String uri, String albumArt) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.duration = duration;
            this.uri = uri;
            this.albumArt = albumArt;
        }

        public long getId() {
            return id;
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public long getDuration() {
            return duration;
        }

        public long getAlbumId() {
            return albumId;
        }

        public String getURI() {

            Uri mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID};
            String selection = MediaStore.Audio.Media._ID + "=?";
            String[] selectionArgs = new String[] {"" + id}; //This is the id you are looking for

            Cursor mediaCursor = getContentResolver().query(mediaContentUri, projection, selection, selectionArgs, null);

            if(mediaCursor.getCount() >= 0) {
                mediaCursor.moveToPosition(0);
//                String title = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
//                String album = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
//                String artist = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
//                long duration = mediaCursor.getLong(mediaCursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                uri = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                //Do something with the data
            }
            mediaCursor.close();
            return uri;

        }

        public String getAlbumArt() {
            String path = "";
//            try {
//                Uri genericArtUri = Uri.parse("content://media/external/audio/albumart");
//                Uri actualArtUri = ContentUris.withAppendedId(genericArtUri, albumId);
//                return actualArtUri.toString();
//            } catch(Exception e) {
//                return null;
//            }
            Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Albums._ID+ "=?",
                    new String[] {String.valueOf(albumId)},
                    null);

            if (cursor.moveToFirst()) {
               path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                // do whatever you need to do

            }
            cursor.close();
            return path;
        }
        HashMap<String,Object> toMap(){
            HashMap<String,Object> songsMap = new HashMap<>();
            songsMap.put("id", id);
            songsMap.put("artist",artist);
            songsMap.put("title",title);
            songsMap.put("album",album);
            songsMap.put("albumId",albumId);
            songsMap.put("duration", duration);
            songsMap.put("uri",uri);
            songsMap.put("albumArt",albumArt);


            return songsMap;
        }

    }
}

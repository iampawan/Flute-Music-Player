package com.mtechviral.musicfinder;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;

import com.avirias.audiofocus.AudioFocusPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;

/**
 * MusicFinderPlugin
 */
public class MusicFinderPlugin implements MethodCallHandler, PluginRegistry.RequestPermissionsResultListener {
  private final MethodChannel channel;

  private static final int REQUEST_CODE_STORAGE_PERMISSION = 3777;

  private Activity activity;
  private Map<String, Object> arguments;
  private boolean executeAfterPermissionGranted;
  private static MusicFinderPlugin instance;
  private Result pendingResult;

  //MusicPlayer
  private static AudioManager am;

  final Handler handler = new Handler();

  AudioFocusPlayer audioFocusPlayer;


  /**
   * Plugin registration.
   */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "music_finder");
    instance = new MusicFinderPlugin(registrar.activity(), channel);
    registrar.addRequestPermissionsResultListener(instance);
    channel.setMethodCallHandler(instance);

  }

  private MusicFinderPlugin(Activity activity, MethodChannel channel) {
    this.activity = activity;
    this.channel = channel;
    this.channel.setMethodCallHandler(this);
    if (MusicFinderPlugin.am == null) {
      MusicFinderPlugin.am = (AudioManager) activity.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + Build.VERSION.RELEASE);
        break;
      case "getSongs":
        pendingResult = result;
        if (!(call.arguments instanceof Map)) {
          throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
        }
        arguments = (Map<String, Object>) call.arguments;
        boolean handlePermission = (boolean) arguments.get("handlePermissions");
        this.executeAfterPermissionGranted = (boolean) arguments.get("executeAfterPermissionGranted");
        checkPermission(handlePermission);
        // result.success(getData());

        break;
      case "play":
        String url = ((HashMap) call.arguments()).get("url").toString();
        Boolean resPlay = play(url);
        result.success(1);
        break;
      case "pause":
        pause();
        result.success(1);
        break;
      case "stop":
        stop();
        result.success(1);
        break;
      case "seek":
        double position = call.arguments();
        seek(position);
        result.success(1);
        break;
      case "mute":
        Boolean muted = call.arguments();
        mute(muted);
        result.success(1);
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private void checkPermission(boolean handlePermission) {
    if (checkSelfPermission(activity) != PackageManager.PERMISSION_GRANTED) {
      if (handlePermission) {
        requestPermissions();
      } else {
        setNoPermissionsError();
      }

    } else {
      pendingResult.success(getData());
      pendingResult = null;
      arguments = null;
    }
  }

  private void scanMusicFiles(File[] files) {
    for (File file: files) {
      if (file.isDirectory())  {
        scanMusicFiles(file.listFiles());
      } else {
        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
                + file.getAbsolutePath())));
      }
    }
  }

  ArrayList<HashMap> getData() {
    MusicFinder mf = new MusicFinder(activity.getContentResolver());

    // Scan all files under Music folder in external storage directory
    scanMusicFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).listFiles());

    mf.prepare();
    List<MusicFinder.Song> allsongs = mf.getAllSongs();
    ArrayList<HashMap> songsMap = new ArrayList<>();
    for (MusicFinder.Song s : allsongs) {
      songsMap.add(s.toMap());
    }
    return songsMap;
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void requestPermissions() {
    activity.requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
        REQUEST_CODE_STORAGE_PERMISSION);
  }

  private boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
    if (Build.VERSION.SDK_INT >= 23) {
      return activity.shouldShowRequestPermissionRationale(permission);
    }
    return false;
  }

  private int checkSelfPermission(Context context) {
    if (Manifest.permission.READ_EXTERNAL_STORAGE == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, android.os.Process.myPid(), Process.myUid());
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int grantResult = grantResults[i];

        if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          if (grantResult == PackageManager.PERMISSION_GRANTED) {
            if (executeAfterPermissionGranted) {
              pendingResult.success(getData());
              pendingResult = null;
              arguments = null;
            }
          } else {
            setNoPermissionsError();
          }
        }
      }
    }
    return false;
  }

  private void setNoPermissionsError() {
    pendingResult.error("permission", "you don't have the user permission to access the camera", null);
    pendingResult = null;
    arguments = null;
  }

  private void mute(Boolean muted) {
    if (MusicFinderPlugin.am == null)
      return;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      MusicFinderPlugin.am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
          muted ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
    } else {
      MusicFinderPlugin.am.setStreamMute(AudioManager.STREAM_MUSIC, muted);
    }
  }

  private void seek(double position) {
    audioFocusPlayer.seekTo((int) (position * 1000));
  }

  private void stop() {
    handler.removeCallbacks(sendData);
    if (audioFocusPlayer != null) {
      audioFocusPlayer.stop();
      audioFocusPlayer.destroy();
    }
  }

  private void pause() {
    audioFocusPlayer.pause();
    handler.removeCallbacks(sendData);
  }

  private Boolean play(String url) {
    if (audioFocusPlayer == null) {
      audioFocusPlayer = new AudioFocusPlayer(activity);
          audioFocusPlayer.setDataSource(Uri.fromFile(new File(url)));
    } else {
      channel.invokeMethod("audio.onDuration", audioFocusPlayer.getDuration());

      audioFocusPlayer.play();
      channel.invokeMethod("audio.onStart", true);
    }

    audioFocusPlayer.onComplete(new Function1<AudioFocusPlayer, Unit>() {
      @Override
      public Unit invoke(AudioFocusPlayer audioFocusPlayer) {
        stop();
        channel.invokeMethod("audio.onComplete", true);
        return Unit.INSTANCE;
      }
    });

    audioFocusPlayer.onError(new Function3<AudioFocusPlayer, Integer, Integer, Boolean>() {
      @Override
      public Boolean invoke(AudioFocusPlayer audioFocusPlayer, Integer integer, Integer integer2) {
        channel.invokeMethod("audio.onError", String.format("{\"what\":%d,\"extra\":%d}", integer, integer2));
        return true;
      }
    });

    handler.post(sendData);

    return true;
  }

  private final Runnable sendData = new Runnable() {
    public void run() {
      try {
        if (!audioFocusPlayer.isPlaying()) {
          handler.removeCallbacks(sendData);
        }
        int time = audioFocusPlayer.getCurrentPosition();
        channel.invokeMethod("audio.onCurrentPosition", time);

        handler.postDelayed(this, 200);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

}

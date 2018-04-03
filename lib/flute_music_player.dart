import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

typedef void TimeChangeHandler(Duration duration);
typedef void ErrorHandler(String message);

class MusicFinder {
  static const MethodChannel _channel = const MethodChannel('music_finder');
  bool _handlePermissions = true;
  bool _executeAfterPermissionGranted = true;

  TimeChangeHandler durationHandler;
  TimeChangeHandler positionHandler;
  VoidCallback startHandler;
  VoidCallback completionHandler;
  ErrorHandler errorHandler;

  MusicFinder() {
    _channel.setMethodCallHandler(platformCallHandler);
    //durationNotifier = new ValueNotifier(new Duration());
  }
  Future<dynamic> play(String url, {bool isLocal: false}) =>
      _channel.invokeMethod('play', {"url": url, "isLocal": isLocal});

  Future<dynamic> pause() => _channel.invokeMethod('pause');

  Future<dynamic> stop() => _channel.invokeMethod('stop');

  Future<dynamic> mute(bool muted) => _channel.invokeMethod('mute', muted);

  Future<dynamic> seek(double seconds) =>
      _channel.invokeMethod('seek', seconds);

  void setDurationHandler(TimeChangeHandler handler) {
    durationHandler = handler;
  }

  void setPositionHandler(TimeChangeHandler handler) {
    positionHandler = handler;
  }

  void setStartHandler(VoidCallback callback) {
    startHandler = callback;
  }

  void setCompletionHandler(VoidCallback callback) {
    completionHandler = callback;
  }

  void setErrorHandler(ErrorHandler handler) {
    errorHandler = handler;
  }

  //Finder

  MusicFinder setHandlePermissions(bool handlePermissions) {
    _handlePermissions = handlePermissions;
    return this;
  }

  MusicFinder setExecuteAfterPermissionGranted(
      bool executeAfterPermissionGranted) {
    _executeAfterPermissionGranted = executeAfterPermissionGranted;
    return this;
  }

  static Future<String> get platformVersion =>
      _channel.invokeMethod('getPlatformVersion');

  static Future<dynamic> allSongs() async {
    var completer = new Completer();

    // At some time you need to complete the future:

    Map params = <String, dynamic>{
      "handlePermissions": true,
      "executeAfterPermissionGranted": true,
    };
    List<dynamic> songs = await _channel.invokeMethod('getSongs', params);
    print(songs.runtimeType);
    var mySongs = songs.map((m) => new Song.fromMap(m)).toList();
    completer.complete(mySongs);
    return completer.future;
  }

  Future platformCallHandler(MethodCall call) async {
    //    print("_platformCallHandler call ${call.method} ${call.arguments}");
    switch (call.method) {
      case "audio.onDuration":
        final duration = new Duration(milliseconds: call.arguments);
        if (durationHandler != null) {
          durationHandler(duration);
        }
        //durationNotifier.value = duration;
        break;
      case "audio.onCurrentPosition":
        if (positionHandler != null) {
          positionHandler(new Duration(milliseconds: call.arguments));
        }
        break;
      case "audio.onStart":
        if (startHandler != null) {
          startHandler();
        }
        break;
      case "audio.onComplete":
        if (completionHandler != null) {
          completionHandler();
        }
        break;
      case "audio.onError":
        if (errorHandler != null) {
          errorHandler(call.arguments);
        }
        break;
      default:
        print('Unknowm method ${call.method} ');
    }
  }
}

class Song {
  int id;
  String artist;
  String title;
  String album;
  int albumId;
  int duration;
  String uri;
  String albumArt;

  Song(this.id, this.artist, this.title, this.album, this.albumId,
      this.duration, this.uri, this.albumArt);
  Song.fromMap(Map m) {
    id = m["id"];
    artist = m["artist"];
    title = m["title"];
    album = m["album"];
    albumId = m["albumId"];
    duration = m["duration"];
    uri = m["uri"];
    albumArt = m["albumArt"];
  }
}

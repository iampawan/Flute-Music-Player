#import "MusicFinderPlugin.h"
#import <music_finder/music_finder-Swift.h>

@implementation MusicFinderPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftMusicFinderPlugin registerWithRegistrar:registrar];
}
@end

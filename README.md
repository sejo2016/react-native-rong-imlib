# react-native-rong-imlib
React Native 融云SDK(仅IMLib)

    
    npm i —save git://github.com/sejo2016/react-native-rong-imlib.git
    react-native link react-native-rong-imlib
    
    import RongIMLib from 'react-native-rong-imlib'
    
#### ios
从node_modules/react-native-rong-imlib下找到添加到build parse->Link Binary With Libraries
* libopencore-amrnb.a
* RongIMLib.framework
* libsqlite3.tbd

增加search path
framework search path和Library search path:$(SRCROOT)/../node_modules/react-native-rong-imlib/ios/RongCloudSDK

#### AppDelegate.m
#import <RongIMLib/RongIMLib.h>

[[RCIMClient sharedRCIMClient] initWithAppKey:@“融云key”];

####Android API

    

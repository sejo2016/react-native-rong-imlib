# react-native-rong-imlib
React Native 融云SDK(仅IMLib)

    
    npm i —save git://github.com/sejo2016/react-native-rong-imlib.git
    react-native link react-native-rong-imlib
    
    import RongIMLib from 'react-native-rong-imlib'
    
### ios
从node_modules/react-native-rong-imlib下找到添加到build parse->Link Binary With Libraries
* libopencore-amrnb.a
* RongIMLib.framework
* libsqlite3.tbd

增加search path
framework search path和Library search path:$(SRCROOT)/../node_modules/react-native-rong-imlib/ios/RongCloudSDK

#### AppDelegate.m
#####import <RongIMLib/RongIMLib.h>
#####[[RCIMClient sharedRCIMClient] initWithAppKey:@“融云key”];

---------------------


### android
/node_nodules/react-native-rong-imlib/../AndroidManifest.xml   添加融云key
#### Android API

###### 1. connect(string token)
###### 2. disconnect()
###### 以下API参数type指聊天类型
```js
        NONE(0, "none"),
        PRIVATE(1, "private"),
        DISCUSSION(2, "discussion"),
        GROUP(3, "group"),
        CHATROOM(4, "chatroom"),
        CUSTOMER_SERVICE(5, "customer_service"),
        SYSTEM(6, "system"),
        APP_PUBLIC_SERVICE(7, "app_public_service"),
        PUBLIC_SERVICE(8, "public_service"),
        PUSH_SERVICE(9, "push_service");
```
###### 3. sendMessage(string type,string targetId,object content,string puchContent,string pushData)
发送消息格式
```js
message = {
    "content":"消息内容",
    "extra":{
        i:"对话ID",
        c:"对话类型[1:单人 2:剧情私聊-角色固定 3:剧情私聊-角色不固定 4:尬聊 11:群聊-角色固定 12:群聊-角色不固定]",
        t:"消息类型[1:本人说 2:角色说 3:切换角色 4:增加剧情 5:事件触发 6:用户点赞 7:收到礼物 8:用户加入 ]",
        r:"角色ID",
        u:"用户ID[默认为0,当消息类型为6或7，8时使用]",
        m:"图片地址，没有图片默认空",
        l:"长度，默认0"
    }
}
```

###### 4. getConversation(string type,string targetId)
###### 5. getConversationList()
###### 6. getLatestMessages(string type,string targetId,number count)
###### 7. removeConversation(string type,string targetId)
###### 8. insertMessage(string type,string targetId,string senderId,object content)
###### 9. clearMessageUnreadStatus(string type,string targetId)
###### 10. startRecordVoice()
###### 11. cancelRecordVoice()
###### 12. finishRecordVoice()
###### 13. startPlayVoice()
###### 14. stopPlayVoice()
###### 15. getConversationNotificationStatus(string type,string targetId)
###### 16. setConversationNotificationStatus(string type,string targetId,boolean isBlock)
###### 17. clearMessages(string type,string targetId)
###### 18. deleteMessagesByTarget(string type,string targetId)
###### 19. deleteMessagesByMsgIds(array targetIds)
###### 20. getRemoteHistoryMessages(string type,string targetId,number dateTime,number count)
###### 21. saveTextMessageDraft(string type,string targetId,string content)
###### 22. getTextMessageDraft(string type,string targetId)
###### 23. addToBlacklist(string userId)
###### 24. removeFromBlacklist(string userId)
###### 25. getBlacklist()
###### 26. createDiscussion(string name,array userIdLists)
###### 27. addMemberToDiscussion(string discussionId,array userIdList)
###### 28. removeMemberFromDiscussion(string discussionId,string userId)
###### 29. quitDiscussion(string discussionId)
###### 30. getDiscussion(string discussionId)
###### 31. setDiscussionName(string discussionId,string name)
###### 32. setDiscussionInviteStatus(string discussionId,number status)



sendCustomizeMessage已实现demo，具体用到的话根据需要更改方法，调整参数


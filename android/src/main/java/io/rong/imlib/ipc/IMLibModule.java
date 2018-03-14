package io.rong.imlib.ipc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imlib.AnnotationNotFoundException;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.RecallNotificationMessage;
import okhttp3.internal.Util;

/**
 * Created by tdzl2003 on 3/31/16.
 */
public class IMLibModule extends ReactContextBaseJavaModule implements RongIMClient.OnReceiveMessageListener, RongIMClient.ConnectionStatusListener, LifecycleEventListener {

    static boolean isIMClientInited = false;

    boolean hostActive = true;

    public IMLibModule(ReactApplicationContext reactContext) {
        super(reactContext);

        if (!isIMClientInited) {
            isIMClientInited = true;
            RongIMClient.init(reactContext.getApplicationContext());
            try {
                RongIMClient.registerMessageType(CustomizeMessage.class);
            } catch (AnnotationNotFoundException e) {
                e.printStackTrace();
            }
        }

        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "RCTRongIMLib";
    }

    @Override
    public void initialize() {
        RongIMClient.setOnReceiveMessageListener(this);
        RongIMClient.setConnectionStatusListener(this);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        RongIMClient.setOnReceiveMessageListener(null);
        RongIMClient.getInstance().disconnect();
    }

    private void sendDeviceEvent(String type, Object arg){
        ReactContext context = this.getReactApplicationContext();
        context.getJSModule(RCTNativeAppEventEmitter.class)
                .emit(type, arg);

    }

    @Override
    public boolean onReceived(Message message, int i) {
        sendDeviceEvent("rongIMMsgRecved", Utils.convertMessage(message));

        if (!hostActive) {
            Context context = getReactApplicationContext();
            NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            MessageContent content = message.getContent();
            String title = content.getUserInfo() != null ? content.getUserInfo().getName() : message.getSenderUserId();

            String contentString = Utils.convertMessageContentToString(content);
            mBuilder.setSmallIcon(context.getApplicationInfo().icon)
                    .setContentTitle(title)
                    .setContentText(contentString)
                    .setTicker(contentString)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL);

            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri.Builder builder = Uri.parse("rong://" + context.getPackageName()).buildUpon();

            builder.appendPath("conversation").appendPath(message.getConversationType().getName())
                    .appendQueryParameter("targetId", message.getTargetId())
                    .appendQueryParameter("title", message.getTargetId());
            intent.setData(builder.build());
            mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

            Notification notification = mBuilder.build();
            mNotificationManager.notify(1000, notification);
        }
        return true;
    }

    RongIMClient client = null;

    @ReactMethod
    public void connect(String token, final Promise promise){
        if (client != null) {
            promise.reject("AlreadyLogined", "Is already logined.");
            return;
        }
        client = RongIMClient.connect(token, new RongIMClient.ConnectCallback() {
            /**
             * Token 错误，在线上环境下主要是因为 Token 已经过期，您需要向 App Server 重新请求一个新的 Token
             */
            @Override
            public void onTokenIncorrect() {
                promise.reject("tokenIncorrect", "Incorrect token provided.");
            }

            /**
             * 连接融云成功
             * @param userid 当前 token
             */
            @Override
            public void onSuccess(String userid) {
                promise.resolve(userid);
            }

            /**
             * 连接融云失败
             * @param errorCode 错误码，可到官网 查看错误码对应的注释
             */
            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void getConversation(final String type, final String targetId, final Promise promise){
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.getConversation(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, new RongIMClient.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                promise.resolve(conversation);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void getConversationList(final Promise promise){
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {

            @Override
            public void onSuccess(List<Conversation> conversations) {
                promise.resolve(Utils.convertConversationList(conversations));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void logout(final Promise promise){
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.logout();
        client = null;
        promise.resolve(null);
    }

    @ReactMethod
    public void disconnect(final Promise promise){
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.disconnect();
        promise.resolve(null);
    }

    @ReactMethod
    public void getLatestMessages(String type, String targetId, int count, final Promise promise) {
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.getLatestMessages(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, count, new RongIMClient.ResultCallback<List<Message>>() {

            @Override
            public void onSuccess(List<Message> messages) {
                promise.resolve(Utils.convertMessageList(messages));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }
        });
    }

    @ReactMethod
        public void removeConversation(final String type, final String targetId, final Promise promise) {
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.removeConversation(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, new RongIMClient.ResultCallback<Boolean>(){
            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
              promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }

            @Override
            public void onSuccess(Boolean message) {
              promise.resolve(message);
            }
        });
    }

    @ReactMethod
    public void sendMessage(final String type, final String targetId, final ReadableMap map, final String pushContent, final String pushData, final Promise promise) {
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        /*if ("image".equals(map.getString("type"))) {
            Utils.getImage(Uri.parse(map.getString("imageUrl")), null, new Utils.ImageCallback(){

                @Override
                public void invoke(@Nullable Bitmap bitmap) {
                    if (bitmap == null){
                        promise.reject("loadImageFailed", "Cannot open image uri ");
                        return;
                    }
                    MessageContent content;
                    try {
                        content = Utils.convertImageMessageContent(getReactApplicationContext(), bitmap);
                    } catch (Throwable e){
                        promise.reject("cacheImageFailed", e);
                        return;
                    }
                    client.sendImageMessage(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, content, pushContent, pushData, new RongIMClient.SendImageMessageCallback() {

                        @Override
                        public void onAttached(Message message) {
                            promise.resolve(Utils.convertMessage(message));
                        }

                        @Override
                        public void onError(Message message, RongIMClient.ErrorCode e) {
                            WritableMap ret = Arguments.createMap();
                            ret.putInt("messageId", message.getMessageId());
                            ret.putInt("errCode", e.getValue());
                            ret.putString("errMsg", e.getMessage());
                            sendDeviceEvent("msgSendFailed", ret);
                        }

                        @Override
                        public void onSuccess(Message message) {
                            sendDeviceEvent("msgSendOk", message.getMessageId());
                        }

                        @Override
                        public void onProgress(Message message, int i) {

                        }
                    });
                }
            });
            return;
        }*/
        client.sendMessage(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, Utils.convertToMessageContent(map), pushContent, pushData, new RongIMClient.SendMessageCallback() {
            @Override
            public void onError(Integer messageId, RongIMClient.ErrorCode e) {
                WritableMap ret = Arguments.createMap();
                ret.putInt("messageId", messageId);
                ret.putInt("errCode", e.getValue());
                ret.putString("errMsg", e.getMessage());
                sendDeviceEvent("msgSendFailed", ret);
            }

            @Override
            public void onSuccess(Integer messageId) {
                sendDeviceEvent("msgSendOk", messageId);

            }

        }, new RongIMClient.ResultCallback<Message>() {
            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }

            @Override
            public void onSuccess(Message message) {
                promise.resolve(Utils.convertMessage(message));
            }

        });
    }

    /**
     * 自定义消息，根据需求更改参数等内容
     * @param type
     * @param targetId
     * @param pushContent
     * @param pushData
     * @param customizeId
     * @param extra
     * @param promise
     */
    @ReactMethod
    public void sendCustomizeMessage(String type, String targetId, String pushContent, String pushData, String customizeId, String extra, final Promise promise){
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }

        CustomizeMessage customizeMessage = CustomizeMessage.obtain(customizeId,"gift","http://www.baidu.com",extra);

        RongIMClient.getInstance().sendMessage(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, customizeMessage, pushContent, pushData, new IRongCallback.ISendMessageCallback() {
            @Override
            public void onAttached(Message message) {

            }

            @Override
            public void onSuccess(Message message) {
                RLog.e("++++customizeMessage++++:", String.valueOf(Utils.convertMessage(message)));
                promise.resolve(Utils.convertMessage(message));
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void insertMessage(String type, String targetId, String senderId, ReadableMap map, final Promise promise) {
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.insertMessage(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, senderId, Utils.convertToMessageContent(map),
                new RongIMClient.ResultCallback<Message>() {
                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                        promise.reject("" + errorCode.getValue(), errorCode.getMessage());
                    }

                    @Override
                    public void onSuccess(Message message) {
                        promise.resolve(Utils.convertMessage(message));
                    }
                });
    }

    @ReactMethod
    public void clearMessageUnreadStatus(String type, String targetId, final Promise promise){
        if (client == null) {
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.clearMessagesUnreadStatus(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                promise.resolve(null);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject("" + errorCode.getValue(), errorCode.getMessage());
            }
        });
    }

    private MediaRecorder recorder;
    private Promise recordPromise;

    private File recordTarget = new File(this.getReactApplicationContext().getFilesDir(), "imlibrecord.amr");

    private long startTime;

    @ReactMethod
    public void startRecordVoice(Promise promise)
    {
        if (recorder != null) {
            cancelRecordVoice();
            return;
        }
        startTime = new Date().getTime();
        recorder = new MediaRecorder();// new出MediaRecorder对象
        // 设置MediaRecorder的音频源为麦克风
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置MediaRecorder录制的音频格式
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        // 设置MediaRecorder录制音频的编码为amr.
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        recorder.setAudioChannels(1);
        recorder.setAudioSamplingRate(8000);

        recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.d("MediaRecord", "OnError: " + what + "" + extra);
            }
        });

        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                Log.d("MediaRecord", "OnInfo: " + what + "" + extra);
            }
        });

        recorder.setOutputFile(recordTarget.toString());

        try {
            recorder.prepare();
        } catch (IOException e) {
            recorder.release();
            recorder = null;
            promise.reject(e);
            return;
        }
        recorder.start();
        recordPromise = promise;
    }

    @ReactMethod
    public void cancelRecordVoice()
    {
        if (recorder == null){
            return;
        }
        recorder.stop();
        recorder.release();
        recorder = null;
        recordPromise.reject("Canceled", "Record was canceled by user.");
        recordPromise = null;
    }

    @ReactMethod
    public void finishRecordVoice()
    {
        if (recorder == null){
            return;
        }
        recorder.stop();
        recorder.release();
        recorder = null;
        FileInputStream inputFile = null;
        try {
            WritableMap ret = Arguments.createMap();

            inputFile = new FileInputStream(recordTarget);
            byte[] buffer = new byte[(int) recordTarget.length()];
            inputFile.read(buffer);
            inputFile.close();
            ret.putString("type", "voice");
            ret.putString("base64", Base64.encodeToString(buffer, Base64.DEFAULT));
            ret.putString("uri", Uri.fromFile(recordTarget).toString());
            ret.putInt("duration", (int)(new Date().getTime() - startTime));
            recordPromise.resolve(ret);
        } catch (IOException e) {
            recordPromise.reject(e);
            e.printStackTrace();
        }
        recordPromise = null;
    }

    private MediaPlayer player;
    private Promise playerPromise;

    @ReactMethod
    public void startPlayVoice(ReadableMap map, Promise promise) {
        if (player != null){
            this.stopPlayVoice();
        }

        String strUri = map.getString("uri");
        player = MediaPlayer.create(this.getReactApplicationContext(), Uri.parse(strUri));
        playerPromise = promise;
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onPlayComplete(mp);
            }
        });
        player.start();
    }

    private void onPlayComplete(MediaPlayer mp) {
        if (player == mp) {
            playerPromise.resolve(null);
            playerPromise = null;
            player.release();
            player = null;
        }
    }

    @ReactMethod
    public void stopPlayVoice() {
        if (player != null) {
            playerPromise.reject("Canceled", "Record was canceled by user.");
            playerPromise = null;
            player.stop();
            player.release();
            player = null;
        }
    }

    @ReactMethod
    public void getConversationNotificationStatus(String type, String targetId, final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.getConversationNotificationStatus(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId,
                new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>(){

                    @Override
                    public void onSuccess(Conversation.ConversationNotificationStatus conversationNotificationStatus) {
                        switch (conversationNotificationStatus) {
                            case DO_NOT_DISTURB:
                                promise.resolve(true);
                                break;
                            default:
                                promise.resolve(false);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                        promise.reject("" + errorCode.getValue(), errorCode.getMessage());
                    }
                });
    }

    @ReactMethod
    public void setConversationNotificationStatus(String type, String targetId, boolean isBlock, final Promise promise) {
        client.setConversationNotificationStatus(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId,
                isBlock ? Conversation.ConversationNotificationStatus.DO_NOT_DISTURB : Conversation.ConversationNotificationStatus.NOTIFY,
                new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {

                    @Override
                    public void onSuccess(Conversation.ConversationNotificationStatus conversationNotificationStatus) {
                        promise.resolve(null);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                        promise.reject("" + errorCode.getValue(), errorCode.getMessage());
                    }
                });
    }

    @ReactMethod
    public void clearMessages(String type, String targetId, final Promise promise) {
        client.clearMessages(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId,
                new  RongIMClient.ResultCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        promise.resolve(null);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                        promise.reject("" + errorCode.getValue(), errorCode.getMessage());
                    }
                });
    }

    @ReactMethod
    public void deleteMessagesByTarget(String type, String targetId, final Promise promise){
        client.deleteMessages(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                promise.resolve(aBoolean);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void deleteMessagesByMsgIds(ReadableArray targetId, final Promise promise){
        client.deleteMessages(Utils.convertToIntArray(targetId), new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                promise.resolve(aBoolean);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void getRemoteHistoryMessages(String type,String targetId,Integer dateTime,Integer count,final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }

        client.getRemoteHistoryMessages(Conversation.ConversationType.valueOf(type.toUpperCase()),targetId,dateTime,count, new RongIMClient.ResultCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                promise.resolve(Utils.convertMessageList(messages));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });

    }

    @ReactMethod
    public void saveTextMessageDraft(String type, String targetId, final String content, final Promise promise){
        client.saveTextMessageDraft(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, content, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                promise.resolve(aBoolean);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void getTextMessageDraft(final String type,final String targetId, final Promise promise){
        client.getTextMessageDraft(Conversation.ConversationType.valueOf(type.toUpperCase()), targetId, new RongIMClient.ResultCallback<String>() {
            @Override
            public void onSuccess(String s) {
                promise.resolve(s);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void setConversationToTop(final String type, final String id, final boolean isTop, final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.setConversationToTop(Conversation.ConversationType.valueOf(type.toUpperCase()), id, isTop, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                promise.resolve(aBoolean);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void addToBlacklist(String userId, final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }

        client.addToBlacklist(userId, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve("add to black list successfully");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void removeFromBlacklist(String userId, final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.removeFromBlacklist(userId, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve("removed from black list successfully");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void getBlacklist(final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.getBlacklist(new RongIMClient.GetBlacklistCallback() {
            @Override
            public void onSuccess(String[] strings) {
                promise.resolve(strings);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void createDiscussion(final String name, final ReadableArray userIdList, final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.createDiscussion(name, Utils.converToStringList(userIdList), new RongIMClient.CreateDiscussionCallback() {
            @Override
            public void onSuccess(String s) {
                promise.resolve(s);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void addMemberToDiscussion(final String discussionId, final ReadableArray userIdList,final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.addMemberToDiscussion(discussionId, Utils.converToStringList(userIdList), new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve("add member to discussion successful");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void removeMemberFromDiscussion(final String discussionId,final String userId,final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.removeMemberFromDiscussion(discussionId, userId, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve("remove member to discussion successful");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void quitDiscussion(final String discussionId,final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.quitDiscussion(discussionId, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve("quit discussion successful");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void getDiscussion(final String discussionId,final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }

        client.getDiscussion(discussionId, new RongIMClient.ResultCallback<Discussion>() {
            @Override
            public void onSuccess(Discussion discussion) {
                promise.resolve(discussion);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void setDiscussionName(final String discussionId, final String name,final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.setDiscussionName(discussionId, name, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve("set discussion name successful");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void setDiscussionInviteStatus(final String discussionId, final Integer status, final Promise promise){
        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }
        client.setDiscussionInviteStatus(discussionId, RongIMClient.DiscussionInviteStatus.setValue(status), new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                promise.resolve("set discussion invite status successful");
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }

    @ReactMethod
    public void recallMessage(final Integer messageId,final Promise promise){

        if (client == null){
            promise.reject("NotLogined", "Must call connect first.");
            return;
        }

        Message message = new Message();
        message.setMessageId(messageId);

        client.recallMessage(message, new RongIMClient.ResultCallback<RecallNotificationMessage>() {
            @Override
            public void onSuccess(RecallNotificationMessage recallNotificationMessage) {
                WritableMap writableMap = Arguments.createMap();
                writableMap.putString("operatorId",recallNotificationMessage.getOperatorId());
                writableMap.putInt("recallTime", (int) recallNotificationMessage.getRecallTime());
                writableMap.putString("originalObjectName",recallNotificationMessage.getOriginalObjectName());
                sendDeviceEvent("recallListener",writableMap);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                promise.reject(""+errorCode.getValue(),errorCode.getMessage());
            }
        });
    }


    @Override
    public void onChanged(ConnectionStatus connectionStatus) {
        WritableMap map = Arguments.createMap();
        map.putInt("code", connectionStatus.getValue());
        map.putString("message", connectionStatus.getMessage());
        this.sendDeviceEvent("rongIMConnectionStatus", map);
    }

    @Override
    public void onHostResume() {
        this.hostActive = true;
    }

    @Override
    public void onHostPause() {
        this.hostActive = false;
    }

    @Override
    public void onHostDestroy() {

    }
}

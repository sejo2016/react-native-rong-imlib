package io.rong.imlib.ipc;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.rong.common.ParcelUtils;
import io.rong.common.RLog;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.MessageContent;

/**
 * Created by toumurasaki on 2018/3/8.
 */

@SuppressLint("ParcelCreator")
@MessageTag(value="app:custom",flag = MessageTag.ISCOUNTED | MessageTag.ISPERSISTED)
public class CustomizeMessage extends MessageContent {

    private static final String TAG = "CustomizeMessage";

    private String customizeId;
    private String name;
    private String imgUrl;
    private String extra;

    public CustomizeMessage(String customizeId,String name,String imgUrl,String extra){
        this.customizeId = customizeId;
        this.name = name;
        this.imgUrl = imgUrl;
        this.extra = extra;
    }

    public CustomizeMessage(Parcel source) {

    }

    public static CustomizeMessage obtain(String customizeId,String title,String imgUrl,String extra){
        return new CustomizeMessage(customizeId,title,imgUrl,extra);
    }

    public static final Creator<CustomizeMessage> CREATOR = new Creator<CustomizeMessage>() {
        @Override
        public CustomizeMessage createFromParcel(Parcel source) {
            return new CustomizeMessage(source);
        }

        @Override
        public CustomizeMessage[] newArray(int size) {
            return new CustomizeMessage[size];
        }
    };

    public CustomizeMessage(byte[] data){
        String jsonStr = null;

        try {
            jsonStr = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e1) {

        }

        try{
            JSONObject jsonObject = new JSONObject(jsonStr);
            if(jsonObject.has("customizeId")){
                setId(jsonObject.optString("customizeId"));
            }
            if (jsonObject.has("name"))
                setName(jsonObject.optString("name"));
            if (jsonObject.has("portraitUri"))
                setImgUrl(jsonObject.optString("portraitUri"));
            if (jsonObject.has("extra"))
                setExtra(jsonObject.optString("extra"));


        }catch(JSONException e){
            RLog.e(TAG, "JSONException " + e.getMessage());
        }
    }

    @Override
    public byte[] encode() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("customizeId",getId());
            jsonObject.put("name",getEmotion(getName()));
            jsonObject.put("customizeUri", getImgUrl());
            jsonObject.put("extra", getExtra());
            if(getJSONUserInfo() != null){
                jsonObject.putOpt("user",getJSONUserInfo());
            }
        }catch (JSONException e){
            Log.e("JSONException",e.getMessage());
        }

        try {
            return jsonObject.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(customizeId);
        dest.writeString(name);
        dest.writeString(imgUrl);
        dest.writeString(extra);
        ParcelUtils.writeToParcel(dest, getUserInfo());
    }


    private String getEmotion(String content) {

        Pattern pattern = Pattern.compile("\\[/u([0-9A-Fa-f]+)\\]");
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int inthex = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, String.valueOf(Character.toChars(inthex)));
        }

        matcher.appendTail(sb);

        return sb.toString();
    }

    public String getId() {
        return customizeId;
    }

    public void setId(String id) {
        this.customizeId = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}

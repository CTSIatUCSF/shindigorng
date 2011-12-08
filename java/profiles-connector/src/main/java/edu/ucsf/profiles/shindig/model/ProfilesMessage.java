package edu.ucsf.profiles.shindig.model;

import java.util.Date;
import java.util.List;

import org.apache.shindig.social.opensocial.model.Message;
import org.apache.shindig.social.opensocial.model.Url;

public class ProfilesMessage implements Message {
    private String appUrl;
    private String body;
    private String bodyId;
    private List<String> collectionIds;
    private String id;
    private String inReplyTo;
    private List<String> recipients;
    private String senderId;
    private Message.Status status;
    private Date timeSent;
    private String title;
    private String titleId;
    private Message.Type type;
    private Date updated;
    private List<Url> urls;
    
    public String getAppUrl() {
        return appUrl;
    }
    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public String getBodyId() {
        return bodyId;
    }
    public void setBodyId(String bodyId) {
        this.bodyId = bodyId;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getInReplyTo() {
        return inReplyTo;
    }
    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }
    public String getSenderId() {
        return senderId;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public Message.Status getStatus() {
        return status;
    }
    public void setStatus(Message.Status status) {
        this.status = status;
    }
    public Date getTimeSent() {
        return timeSent;
    }
    public void setTimeSent(Date timeSent) {
        this.timeSent = timeSent;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitleId() {
        return titleId;
    }
    public void setTitleId(String titleId) {
        this.titleId = titleId;
    }
    public Message.Type getType() {
        return type;
    }
    public void setType(Message.Type type) {
        this.type = type;
    }
    public Date getUpdated() {
        return updated;
    }
    public void setUpdated(Date updated) {
        this.updated = updated;
    }
    public List<String> getCollectionIds() {
        return collectionIds;
    }
    public void setCollectionIds(List<String> collectionIds) {
        this.collectionIds = collectionIds;
    }
    public List<String> getRecipients() {
        return recipients;
    }
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }
    
    public List<String> getReplies() {
        return null;
    }
    
    public String sanitizeHTML(String html) {
        return html;
    }
    public List<Url> getUrls() {
        return urls;
    }
    public void setUrls(List<Url> urls) {
        this.urls = urls;
    }
    
}

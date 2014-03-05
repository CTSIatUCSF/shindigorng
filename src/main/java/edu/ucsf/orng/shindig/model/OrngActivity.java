package edu.ucsf.orng.shindig.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.shindig.social.opensocial.model.Activity;
import org.apache.shindig.social.opensocial.model.MediaItem;

public class OrngActivity implements Activity {

	private String appId;
	private String body;
	private String bodyId;
	private String externalId;
	private String id;
	private List<MediaItem> mediaItems;
	private Float priority;
	private Long postedTime;
	private String streamFaviconUrl;
	private String streamSourceUrl;
	private String streamTitle;
	private String streamUrl;
	private Map<String, String> templateParams;
	private String title;
	private String titleId;
	private String url;
	private Date updated;
	private String userId;

	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getExternalId() {
		return externalId;
	}
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public List<MediaItem> getMediaItems() {
		return mediaItems;
	}
	public void setMediaItems(List<MediaItem> mediaItems) {
		this.mediaItems = mediaItems;
	}
	public Long getPostedTime() {
		return postedTime;
	}
	public void setPostedTime(Long postedTime) {
		this.postedTime = postedTime;
	}
	public String getStreamUrl() {
		return streamUrl;
	}
	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getBodyId() {
		return bodyId;
	}
	public void setBodyId(String bodyId) {
		this.bodyId = bodyId;
	}
	public Float getPriority() {
		return priority;
	}
	public void setPriority(Float priority) {
		this.priority = priority;
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
	public String getStreamFaviconUrl() {
		return streamFaviconUrl;
	}
	public void setStreamFaviconUrl(String streamFaviconUrl) {
		this.streamFaviconUrl = streamFaviconUrl;
	}
	public String getStreamTitle() {
		return streamTitle;
	}
	public void setStreamTitle(String streamTitle) {
		this.streamTitle = streamTitle;
	}
	public String getStreamSourceUrl() {
		return streamSourceUrl;
	}
	public void setStreamSourceUrl(String streamSourceUrl) {
		this.streamSourceUrl = streamSourceUrl;
	}
	public Map<String, String> getTemplateParams() {
		return templateParams;
	}
	public void setTemplateParams(Map<String, String> templateParams) {
		this.templateParams = templateParams;
	}
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}
	
}

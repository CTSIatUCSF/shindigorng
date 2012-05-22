package edu.ucsf.orng.shindig.model;

import java.util.Date;
import java.util.List;

import org.apache.shindig.social.opensocial.model.MessageCollection;
import org.apache.shindig.social.opensocial.model.Url;

public class OrngMessageCollection implements MessageCollection {
    private String id;
    private Integer unread;
    private List<Url> urls;
    private String title;
    private Integer total;
    private Date updated;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Integer getUnread() {
        return unread;
    }
    public void setUnread(Integer unread) {
        this.unread = unread;
    }
    public List<Url> getUrls() {
        return urls;
    }
    public void setUrls(List<Url> urls) {
        this.urls = urls;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Integer getTotal() {
        return total;
    }
    public void setTotal(Integer total) {
        this.total = total;
    }
    public Date getUpdated() {
        return updated;
    }
    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}

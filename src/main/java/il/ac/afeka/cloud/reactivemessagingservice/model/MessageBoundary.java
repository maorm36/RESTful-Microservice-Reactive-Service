package il.ac.afeka.cloud.reactivemessagingservice.model;

import java.time.ZonedDateTime;
import java.util.Map;

public class MessageBoundary {

    private String id;
    private String target;
    private String sender;
    private String title;
    private ZonedDateTime publicationTimestamp;
    private Boolean urgent;
    private Map<String, Object> moreDetails;

    public MessageBoundary() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ZonedDateTime getPublicationTimestamp() { return publicationTimestamp; }
    public void setPublicationTimestamp(ZonedDateTime publicationTimestamp) { this.publicationTimestamp = publicationTimestamp; }

    public Boolean getUrgent() { return urgent; }
    public void setUrgent(Boolean urgent) { this.urgent = urgent; }

    public Map<String, Object> getMoreDetails() { return moreDetails; }
    public void setMoreDetails(Map<String, Object> moreDetails) { this.moreDetails = moreDetails; }
}

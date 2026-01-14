package il.ac.afeka.cloud.reactivemessagingservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Document(collection = "MESSAGES")
public class MessageEntity {

    @Id
    private String id;

    private String target;
    private String sender;
    private String title;
    private Date publicationTimestamp;
    private boolean urgent;
    private Map<String, Object> moreDetails;

    public MessageEntity() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getPublicationTimestamp() {
        return publicationTimestamp;
    }

    public void setPublicationTimestamp(Date publicationTimestamp) {
        this.publicationTimestamp = publicationTimestamp;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public Map<String, Object> getMoreDetails() {
        return moreDetails;
    }

    public void setMoreDetails(Map<String, Object> moreDetails) {
        this.moreDetails = moreDetails;
    }
}

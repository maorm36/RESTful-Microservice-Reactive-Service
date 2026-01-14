package il.ac.afeka.cloud.reactivemessagingservice.model;

import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Component
public class MessageConverter {

    public MessageBoundary toBoundary(MessageEntity entity) {
        if (entity == null) {
            return null;
        }
        MessageBoundary rv = new MessageBoundary();
        rv.setId(entity.getId());
        rv.setTarget(entity.getTarget());
        rv.setSender(entity.getSender());
        rv.setTitle(entity.getTitle());
        if (entity.getPublicationTimestamp() != null) {
            rv.setPublicationTimestamp(ZonedDateTime.ofInstant(
                    entity.getPublicationTimestamp().toInstant(),
                    ZoneId.systemDefault()
            ));
        }
        rv.setUrgent(entity.isUrgent());
        rv.setMoreDetails(entity.getMoreDetails());
        return rv;
    }

    public MessageEntity toNewEntity(
            MessageBoundary input,
            String id,
            Instant publicationTimestamp,
            boolean urgent,
            Map<String, Object> moreDetails) {
        MessageEntity e = new MessageEntity();
        e.setId(id);
        e.setTarget(input.getTarget());
        e.setSender(input.getSender());
        e.setTitle(input.getTitle());
        e.setPublicationTimestamp(Date.from(publicationTimestamp));
        e.setUrgent(urgent);
        e.setMoreDetails(moreDetails);
        return e;
    }
}

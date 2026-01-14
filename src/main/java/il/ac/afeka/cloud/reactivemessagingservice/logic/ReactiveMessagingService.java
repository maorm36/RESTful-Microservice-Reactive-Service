package il.ac.afeka.cloud.reactivemessagingservice.logic;

import il.ac.afeka.cloud.reactivemessagingservice.model.MessageBoundary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveMessagingService {
    Mono<MessageBoundary> create(MessageBoundary input);

    Flux<MessageBoundary> getAll(int page, int size);

    Flux<MessageBoundary> getByRecipient(String recipientEmail, int page, int size);

    Flux<MessageBoundary> getBySender(String senderEmail, int page, int size);

    Mono<MessageBoundary> getById(String id); // bonus

    Flux<MessageBoundary> getUrgent(int page, int size); // bonus

    Flux<MessageBoundary> getUrgentByRecipient(String recipientEmail, int page, int size); // bonus

    Flux<MessageBoundary> getUrgentBySender(String senderEmail, int page, int size); // bonus

    Mono<Void> deleteAll();
}

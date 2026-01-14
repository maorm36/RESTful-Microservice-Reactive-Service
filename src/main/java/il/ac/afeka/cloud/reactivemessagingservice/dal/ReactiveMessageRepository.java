package il.ac.afeka.cloud.reactivemessagingservice.dal;

import il.ac.afeka.cloud.reactivemessagingservice.model.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ReactiveMessageRepository extends ReactiveMongoRepository<MessageEntity, String> {

    Flux<MessageEntity> findAllByIdNotNull(Pageable pageable);

    Flux<MessageEntity> findAllByTarget(String target, Pageable pageable);

    Flux<MessageEntity> findAllBySender(String sender, Pageable pageable);

    Flux<MessageEntity> findAllByUrgentIsTrue(Pageable pageable);

    Flux<MessageEntity> findAllByUrgentIsTrueAndTarget(String target, Pageable pageable);

    Flux<MessageEntity> findAllByUrgentIsTrueAndSender(String sender, Pageable pageable);
}

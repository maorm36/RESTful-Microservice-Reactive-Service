package il.ac.afeka.cloud.reactivemessagingservice.logic;

import il.ac.afeka.cloud.reactivemessagingservice.dal.ReactiveMessageRepository;
import il.ac.afeka.cloud.reactivemessagingservice.error.BadRequestException;
import il.ac.afeka.cloud.reactivemessagingservice.model.MessageBoundary;
import il.ac.afeka.cloud.reactivemessagingservice.model.MessageConverter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReactiveMessagingServiceImpl implements ReactiveMessagingService {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;

    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("publicationTimestamp"), Sort.Order.asc("id"));

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ReactiveMessageRepository repo;
    private final MessageConverter converter;

    public ReactiveMessagingServiceImpl(ReactiveMessageRepository repo, MessageConverter converter) {
        this.repo = repo;
        this.converter = converter;
    }

    @Override
    public Mono<MessageBoundary> create(MessageBoundary input) {
        return Mono.justOrEmpty(input)
                .switchIfEmpty(Mono.error(new BadRequestException("Message body is required")))
                .map(boundary -> {
                    // Validate + normalize (exceptions here become onError because they run inside the chain)
                    boundary.setTarget(validateEmail("target", boundary.getTarget()));
                    boundary.setSender(validateEmail("sender", boundary.getSender()));
                    validateNotBlank("title", boundary.getTitle());
                    return boundary;
                })
                // urgent is required (Boolean must not be null)
                .filter(boundary -> boundary.getUrgent() != null)
                .switchIfEmpty(Mono.error(new BadRequestException("Urgent field is required")))
                .map(boundary -> {
                    boolean urgent = boundary.getUrgent();

                    Map<String, Object> moreDetails =
                            boundary.getMoreDetails() != null ? boundary.getMoreDetails() : Collections.emptyMap();

                    String id = UUID.randomUUID().toString();
                    Instant publicationTimestamp = Instant.now();

                    return converter.toNewEntity(boundary, id, publicationTimestamp, urgent, moreDetails);
                })
                .flatMap(repo::save)
                .map(converter::toBoundary);
    }

    @Override
    public Flux<MessageBoundary> getAll(int page, int size) {
        return Mono.just(page)
                .map(p -> pageRequest(p, size))
                .flatMapMany(repo::findAllByIdNotNull)
                .map(converter::toBoundary);
    }

    @Override
    public Flux<MessageBoundary> getByRecipient(String recipientEmail, int page, int size) {
        return Mono.just(page)
                .map(p -> pageRequest(p, size))
                .flatMapMany(pr -> repo.findAllByTarget(validateEmail("recipientEmail", recipientEmail), pr))
                .map(converter::toBoundary);
    }

    @Override
    public Flux<MessageBoundary> getBySender(String senderEmail, int page, int size) {
        return Mono.just(page)
                .map(p -> pageRequest(p, size))
                .flatMapMany(pr -> repo.findAllBySender(validateEmail("senderEmail", senderEmail), pr))
                .map(converter::toBoundary);
    }

    @Override
    public Mono<MessageBoundary> getById(String id) {
        return Mono.justOrEmpty(id)
                .filter(v -> !v.isBlank())
                .switchIfEmpty(Mono.error(new BadRequestException("id value is required")))
                .flatMap(repo::findById)
                .map(converter::toBoundary);
    }

    @Override
    public Flux<MessageBoundary> getUrgent(int page, int size) {
        return Mono.just(page)
                .map(p -> pageRequest(p, size))
                .flatMapMany(repo::findAllByUrgentIsTrue)
                .map(converter::toBoundary);
    }

    @Override
    public Flux<MessageBoundary> getUrgentByRecipient(String recipientEmail, int page, int size) {
        return Mono.just(page)
                .map(p -> pageRequest(p, size))
                .flatMapMany(pr -> repo.findAllByUrgentIsTrueAndTarget(validateEmail("recipientEmail", recipientEmail), pr))
                .map(converter::toBoundary);
    }

    @Override
    public Flux<MessageBoundary> getUrgentBySender(String senderEmail, int page, int size) {
        return Mono.just(page)
                .map(p -> pageRequest(p, size))
                .flatMapMany(pr -> repo.findAllByUrgentIsTrueAndSender(validateEmail("senderEmail", senderEmail), pr))
                .map(converter::toBoundary);
    }

    @Override
    public Mono<Void> deleteAll() {
        return repo.deleteAll();
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        int p = (page == null ? DEFAULT_PAGE : page);
        int s = (size == null ? DEFAULT_SIZE : size);

        if (p < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (s <= 0) {
            throw new BadRequestException("size must be > 0");
        }

        return PageRequest.of(p, s, DEFAULT_SORT);
    }

    private String validateEmail(String field, String email) {
        String normalized = normalizeEmail(email);

        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException(field + " must not be blank");
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException(field + " must be a valid email");
        }

        return normalized;
    }

    private void validateNotBlank(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " must not be empty");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;

        String decoded = URLDecoder.decode(email, StandardCharsets.UTF_8);

        return decoded.trim().toLowerCase(Locale.ROOT);
    }
}

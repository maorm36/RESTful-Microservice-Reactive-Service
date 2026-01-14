package il.ac.afeka.cloud.reactivemessagingservice.api;

import il.ac.afeka.cloud.reactivemessagingservice.error.BadRequestException;
import il.ac.afeka.cloud.reactivemessagingservice.logic.ReactiveMessagingService;
import il.ac.afeka.cloud.reactivemessagingservice.model.MessageBoundary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/messages")
public class ReactiveMessagingController {

    private final ReactiveMessagingService service;

    public ReactiveMessagingController(ReactiveMessagingService service) {
        this.service = service;
    }

    // POST /messages
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<MessageBoundary> create(@RequestBody MessageBoundary body) {
        return service.create(body);
    }

    // GET /messages?size={size}&page={page}
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MessageBoundary> getAll(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "value", required = false) String value,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        if ((search == null || search.isBlank()) && (value == null || value.isBlank())) {
            return service.getAll(page, size);
        }

        throw new BadRequestException("Unsupported inputs");
    }

    // GET /messages?search=byRecipient&value={recipientEmail}&size={size}&page={page}
    @GetMapping(params = {"search=byRecipient", "value"},
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MessageBoundary> getByRecipient(
            @RequestParam(name = "value", required = false) String recipientEmail,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        return service.getByRecipient(recipientEmail, page, size);

    }

    // GET /messages?search=bySender&value={senderEmail}&size={size}&page={page}
    @GetMapping(params = {"search=bySender", "value"},
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MessageBoundary> getBySender(
            @RequestParam(name = "value", required = false) String senderEmail,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        return service.getBySender(senderEmail, page, size);

    }

    @DeleteMapping
    public Mono<Void> deleteAll() {
        return service.deleteAll();
    }

    // ##############
    // BONUS SECTION:
    // ##############

    // GET /messages?search=byId&value={id}
    @GetMapping(params = {"search=byId", "value"},
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Mono<MessageBoundary> getById(
            @RequestParam("value") String id)
    {
        return service.getById(id);
    }

    // GET /messages?search=byUrgent&size={size}&page={page}
    @GetMapping(params = {"search=byUrgent"},
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MessageBoundary> getUrgent(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return service.getUrgent(page, size);
    }

    // GET /messages?search=urgentOnlyByRecipient&value={recipientEmail}&size={size}&page={page}
    @GetMapping(params = {"search=urgentOnlyByRecipient", "value"},
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MessageBoundary> getUrgentByRecipient(
            @RequestParam(name = "value", required = false) String recipientEmail,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return service.getUrgentByRecipient(recipientEmail, page, size);
    }

    // GET /messages?search=urgentOnlyBySender&value={senderEmail}&size={size}&page={page}
    @GetMapping(params = {"search=urgentOnlyBySender", "value"},
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MessageBoundary> getUrgentBySender(
            @RequestParam(name = "value", required = false) String senderEmail,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return service.getUrgentBySender(senderEmail, page, size);
    }

}

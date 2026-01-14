package il.ac.afeka.cloud.reactivemessagingservice;

import il.ac.afeka.cloud.reactivemessagingservice.model.MessageBoundary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class ReactiveMessagingServiceApplicationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final ParameterizedTypeReference<ServerSentEvent<MessageBoundary>> SSE_MESSAGE = new ParameterizedTypeReference<>() {};

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void cleanDb() {
        deleteAllMessages();
    }

    // ------------------------------
    // Happy-path tests
    // ------------------------------

    @Test
    void createMessage_returnsPersistedMessage() {
        MessageBoundary request = newMessage(
                "target.user@example.com",
                "sender.user@example.com",
                "Hello " + UUID.randomUUID(),
                true
        );

        MessageBoundary created = postMessage(request);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotBlank();
        assertThat(created.getTarget()).isEqualTo(request.getTarget());
        assertThat(created.getSender()).isEqualTo(request.getSender());
        assertThat(created.getTitle()).isEqualTo(request.getTitle());
        assertThat(created.getUrgent()).isEqualTo(request.getUrgent());
        assertThat(created.getMoreDetails()).containsEntry("key", request.getMoreDetails().get("key"));
        // Converter persists only the LocalDate (java.sql.Date). Verify date-level equality.
        assertThat(created.getPublicationTimestamp().toLocalDate())
                .isEqualTo(request.getPublicationTimestamp().toLocalDate());
    }

    @Test
    void search_byId_returnsSingleMessage() {
        MessageBoundary created = postMessage(newMessage(
                "id.target@example.com",
                "id.sender@example.com",
                "Find me by id",
                false
        ));

        List<MessageBoundary> result = getMessages("/messages?search=byId&value=" + created.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(created.getId());
    }

    @Test
    void getAll_withPaging_returnsPages() {
        for (int i = 0; i < 15; i++) {
            postMessage(newMessage(
                    "user" + i + "@example.com",
                    "sender" + i + "@example.com",
                    "msg-" + i,
                    i % 2 == 0
            ));
        }

        List<MessageBoundary> page1 = getMessages("/messages?page=0&size=10");
        List<MessageBoundary> page2 = getMessages("/messages?page=1&size=10");

        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(5);

        assertThat(page1)
                .extracting(MessageBoundary::getId)
                .doesNotContainAnyElementsOf(page2.stream().map(MessageBoundary::getId).toList());
    }

    @Test
    void search_byRecipient_trimsAndLowercasesValue() {
        postMessage(newMessage("recipient@example.com", "s1@example.com", "t-1", false));
        postMessage(newMessage("recipient@example.com", "s2@example.com", "t-2", true));
        postMessage(newMessage("other@example.com", "s3@example.com", "t-3", true));

        List<MessageBoundary> result = getMessages("/messages?search=byRecipient&value=recipient@example.com&page=0&size=10");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> "recipient@example.com".equalsIgnoreCase(m.getTarget()));
    }

    @Test
    void search_bySender_trimsAndLowercasesValue() {
        postMessage(newMessage("t1@example.com", "team.sender@example.com", "s-1", false));
        postMessage(newMessage("t2@example.com", "team.sender@example.com", "s-2", true));
        postMessage(newMessage("t3@example.com", "other.sender@example.com", "s-3", true));

        List<MessageBoundary> result = getMessages("/messages?search=bySender&value=team.sender@example.com&page=0&size=10");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> "team.sender@example.com".equalsIgnoreCase(m.getSender()));
    }

    @Test
    void urgentOnly_byRecipient_returnsOnlyUrgent() {
        postMessage(newMessage("urgent.recipient@example.com", "s1@example.com", "u-1", true));
        postMessage(newMessage("urgent.recipient@example.com", "s2@example.com", "u-2", false));
        postMessage(newMessage("other@example.com", "s3@example.com", "u-3", true));

        List<MessageBoundary> result = getMessages("/messages?search=urgentOnlyByRecipient&value=urgent.recipient@example.com&page=0&size=10");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTarget()).isEqualToIgnoringCase("urgent.recipient@example.com");
        assertThat(Boolean.TRUE).isEqualTo(result.getFirst().getUrgent());
    }

    @Test
    void urgentOnly_bySender_returnsOnlyUrgent() {
        postMessage(newMessage("t1@example.com", "urgent.sender@example.com", "u-1", true));
        postMessage(newMessage("t2@example.com", "urgent.sender@example.com", "u-2", false));
        postMessage(newMessage("t3@example.com", "other@example.com", "u-3", true));

        List<MessageBoundary> result = getMessages("/messages?search=urgentOnlyBySender&value=urgent.sender@example.com&page=0&size=10");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSender()).isEqualToIgnoringCase("urgent.sender@example.com");
        assertThat(Boolean.TRUE).isEqualTo(result.getFirst().getUrgent());
    }

    @Test
    void urgentOnly_global_returnsOnlyUrgent() {
        postMessage(newMessage("t1@example.com", "s1@example.com", "u-1", true));
        postMessage(newMessage("t2@example.com", "s2@example.com", "u-2", false));
        postMessage(newMessage("t3@example.com", "s3@example.com", "u-3", true));

        List<MessageBoundary> result = getMessages("/messages?search=byUrgent&page=0&size=10");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> Boolean.TRUE.equals(m.getUrgent()));
    }

    @Test
    void deleteAll_removesAllMessages() {
        postMessage(newMessage("t1@example.com", "s1@example.com", "a", false));
        postMessage(newMessage("t2@example.com", "s2@example.com", "b", true));

        deleteAllMessages();

        List<MessageBoundary> remaining = getMessages("/messages?page=1&size=10");
        assertThat(remaining).isEmpty();
    }

    // ------------------------------
    // Validity / error-path tests
    // ------------------------------

    @Test
    void createMessage_invalidSenderEmail_returnsBadRequest() {
        MessageBoundary bad = newMessage("t1@example.com", "not-an-email", "bad", false);

        webTestClient
                .post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.ALL)
                .bodyValue(bad)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getAll_invalidPaging_returnsBadRequest() {
        webTestClient
                .get()
                .uri("/messages?page=0&size=-1")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().
                isBadRequest();

        webTestClient
                .get()
                .uri("/messages?page=0&size=0")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().
                isBadRequest();

        webTestClient
                .get()
                .uri("/messages?page=1&size=-1")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().
                isBadRequest();

        webTestClient
                .get()
                .uri("/messages?page=1&size=0")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().
                isBadRequest();
    }

    @Test
    void search_missingValue_returnsBadRequest() {
        webTestClient
                .get()
                .uri("/messages?search=byRecipient")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void search_unknownSearchType_returnsBadRequest() {
        webTestClient
                .get()
                .uri("/messages?search=byTitle&value=hello")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void search_byId_notFound_returnsOk() {
        webTestClient
                .get()
                .uri("/messages?search=byId&value=" + UUID.randomUUID())
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isOk();
    }

    // ------------------------------
    // Helpers
    // ------------------------------

    private void deleteAllMessages() {
        webTestClient
                .delete()
                .uri("/messages")
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    private MessageBoundary postMessage(MessageBoundary message) {
        MessageBoundary created = webTestClient
                .post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                // Do NOT constrain Accept to application/json: the controller produces text/event-stream.
                .accept(MediaType.ALL)
                .bodyValue(message)
                .exchange()
                .expectStatus().isOk()
                .returnResult(SSE_MESSAGE)
                .getResponseBody()
                .map(ServerSentEvent::data)
                .filter(Objects::nonNull)
                .blockFirst(TIMEOUT);

        assertThat(created)
                .as("POST /messages should return a single event with the created message")
                .isNotNull();

        return created;
    }

    private List<MessageBoundary> getMessages(String uri) {
        List<MessageBoundary> list = webTestClient
                .get()
                .uri(uri)
                .accept(MediaType.ALL)
                .exchange()
                .expectStatus().isOk()
                .returnResult(SSE_MESSAGE)
                .getResponseBody()
                .map(ServerSentEvent::data)
                .filter(Objects::nonNull)
                .collectList()
                .block(TIMEOUT);

        return list == null ? List.of() : list;
    }

    private MessageBoundary newMessage(String target, String sender, String title, boolean urgent) {
        MessageBoundary boundary = new MessageBoundary();
        boundary.setId(null);
        boundary.setTarget(target);
        boundary.setSender(sender);
        boundary.setTitle(title);
        boundary.setUrgent(urgent);
        boundary.setPublicationTimestamp(ZonedDateTime.now(ZoneOffset.UTC));

        Map<String, Object> details = new HashMap<>();
        details.put("key", "value-" + UUID.randomUUID().toString().substring(0, 8));
        boundary.setMoreDetails(details);

        return boundary;
    }
}

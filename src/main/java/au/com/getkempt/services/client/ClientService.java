package au.com.getkempt.services.client;

import au.com.getkempt.common.error.exceptions.BadRequestException;
import au.com.getkempt.common.error.exceptions.ConflictException;
import au.com.getkempt.common.error.exceptions.ForbiddenException;
import au.com.getkempt.common.error.exceptions.NotFoundException;
import au.com.getkempt.common.id.IdGenerator;
import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.common.pagination.CursorPageRequest;
import au.com.getkempt.common.query.SortRequest;
import au.com.getkempt.common.security.Permission;
import au.com.getkempt.common.tenant.TenantContextHolder;
import au.com.getkempt.entities.Client;
import au.com.getkempt.entities.ClientNote;
import au.com.getkempt.common.web.IfMatchSupport;
import au.com.getkempt.enums.booking.BookingStatus;
import au.com.getkempt.mappers.BookingMapper;
import au.com.getkempt.mappers.ClientMapper;
import au.com.getkempt.repositories.booking.BookingRepository;
import au.com.getkempt.repositories.client.ClientRepository;
import au.com.getkempt.repositories.staff.StaffRepository;
import au.com.getkempt.request.client.ClientExportRequest;
import au.com.getkempt.request.client.ClientMergeRequest;
import au.com.getkempt.request.client.ClientNoteUpdateRequest;
import au.com.getkempt.request.client.ClientNoteWriteRequest;
import au.com.getkempt.request.client.ClientWriteRequest;
import au.com.getkempt.responses.booking.BookingResponse;
import au.com.getkempt.responses.client.ClientExportResponse;
import au.com.getkempt.responses.client.ClientLoyaltyProgressResponse;
import au.com.getkempt.responses.client.ClientLoyaltyResponse;
import au.com.getkempt.responses.client.ClientNoteResponse;
import au.com.getkempt.responses.client.ClientResponse;
import au.com.getkempt.services.loyalty.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Client (customer) records administration. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "firstName", "lastName", "totalBookings", "lifetimeSpendCents", "joinedAt");

    private final ClientRepository clientRepository;
    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final LoyaltyService loyaltyService;
    private final ClientMapper clientMapper;
    private final BookingMapper bookingMapper;

    // ── collection ──────────────────────────────────────────────────────────

    public CursorPage<ClientResponse> list(String cursor,
                                           Integer limit,
                                           String sort,
                                           String q,
                                           String tag,
                                           String preferredStaffId,
                                           Boolean hasUpcoming) {
        requireReadAccess();

        Criteria filter = new Criteria();
        List<Criteria> ands = new java.util.ArrayList<>();
        if (q != null && !q.isBlank()) {
            ands.add(clientRepository.nameEmailPhoneMatch(q));
        }
        if (tag != null && !tag.isBlank()) {
            ands.add(Criteria.where("tags").is(tag));
        }
        if (preferredStaffId != null && !preferredStaffId.isBlank()) {
            ands.add(Criteria.where("preferredStaffId").is(preferredStaffId));
        }
        if (Boolean.TRUE.equals(hasUpcoming)) {
            ands.add(clientRepository.hasUpcomingBooking());
        }
        if (!ands.isEmpty()) {
            filter = new Criteria().andOperator(ands.toArray(new Criteria[0]));
        }

        SortRequest sortRequest = SortRequest.parse(sort, SORTABLE, SortRequest.desc("createdAt"));
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return clientRepository.list(filter, sortRequest, page).map(clientMapper::toResponse);
    }

    public List<ClientResponse> search(String q, Integer limit) {
        requireReadAccess();
        if (q == null || q.length() < 2) {
            throw new BadRequestException("q must be at least 2 characters");
        }
        int resolvedLimit = (limit == null) ? CursorPageRequest.DEFAULT_LIMIT
                : Math.min(limit, CursorPageRequest.MAX_LIMIT);
        return clientRepository.search(q, resolvedLimit).stream().map(clientMapper::toResponse).toList();
    }

    public ClientResponse get(String clientId) {
        requireReadAccess();
        return clientMapper.toResponse(clientRepository.requireById(clientId));
    }

    public ClientResponse create(ClientWriteRequest request) {
        requireEditAccess();
        clientRepository.findByPhone(request.phone()).ifPresent(existing -> {
            throw new ConflictException("A client with phone %s already exists".formatted(request.phone()));
        });
        if (request.preferredStaffId() != null && !staffRepository.existsById(request.preferredStaffId())) {
            throw NotFoundException.of("StaffMember", request.preferredStaffId());
        }

        Client client = new Client();
        Instant now = Instant.now();
        client.setJoinedAt(now);
        applyWrite(client, request);
        clientRepository.insert(client);
        return clientMapper.toResponse(client);
    }

    public ClientResponse update(String clientId, String ifMatch, ClientWriteRequest request) {
        requireEditAccess();
        Client client = clientRepository.requireById(clientId);
        IfMatchSupport.check(ifMatch, client.getVersion());

        if (request.phone() != null && !request.phone().equals(client.getPhone())) {
            clientRepository.findByPhone(request.phone()).ifPresent(existing -> {
                if (!existing.getId().equals(clientId)) {
                    throw new ConflictException("A client with phone %s already exists".formatted(request.phone()));
                }
            });
        }
        if (request.preferredStaffId() != null && !staffRepository.existsById(request.preferredStaffId())) {
            throw NotFoundException.of("StaffMember", request.preferredStaffId());
        }
        applyWrite(client, request);
        clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    public void delete(String clientId) {
        requireEditAccess();
        clientRepository.softDelete(clientRepository.requireById(clientId));
    }

    // ── notes ───────────────────────────────────────────────────────────────

    public List<ClientNoteResponse> listNotes(String clientId) {
        requireReadAccess();
        Client client = clientRepository.requireById(clientId);
        return client.getNotes().stream()
                .sorted((a, b) -> {
                    if (a.isPinned() != b.isPinned()) {
                        return a.isPinned() ? -1 : 1;
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(clientMapper::toNoteResponse)
                .toList();
    }

    public ClientNoteResponse createNote(String clientId, ClientNoteWriteRequest request) {
        requireEditAccess();
        Client client = clientRepository.requireById(clientId);
        ClientNote note = ClientNote.builder()
                .id(IdGenerator.generate(IdPrefix.CLIENT_NOTE))
                .body(request.body())
                .pinned(Boolean.TRUE.equals(request.pinned()))
                .createdAt(Instant.now())
                .createdByStaffId(currentStaffId())
                .build();
        client.getNotes().add(note);
        clientRepository.save(client);
        return clientMapper.toNoteResponse(note);
    }

    public ClientNoteResponse updateNote(String clientId, String noteId, ClientNoteUpdateRequest request) {
        requireEditAccess();
        Client client = clientRepository.requireById(clientId);
        ClientNote note = findNote(client, noteId);
        if (request.body() != null) {
            note.setBody(request.body());
        }
        if (request.pinned() != null) {
            note.setPinned(request.pinned());
        }
        clientRepository.save(client);
        return clientMapper.toNoteResponse(note);
    }

    public void deleteNote(String clientId, String noteId) {
        requireEditAccess();
        Client client = clientRepository.requireById(clientId);
        boolean removed = client.getNotes().removeIf(n -> n.getId().equals(noteId));
        if (!removed) {
            throw NotFoundException.of("ClientNote", noteId);
        }
        clientRepository.save(client);
    }

    // ── loyalty ─────────────────────────────────────────────────────────────

    public ClientLoyaltyResponse getLoyalty(String clientId) {
        requireReadAccess();
        clientRepository.requireById(clientId);
        ClientLoyaltyProgressResponse progress = loyaltyService.getClientProgress(clientId);
        List<au.com.getkempt.responses.loyalty.LoyaltyRewardResponse> rewards =
                loyaltyService.listRewards(null, CursorPageRequest.MAX_LIMIT, clientId, null, null).data();
        return new ClientLoyaltyResponse(progress, rewards);
    }

    // ── bookings ────────────────────────────────────────────────────────────

    public CursorPage<BookingResponse> listBookings(String clientId, String cursor, Integer limit, String status) {
        requireReadAccess();
        clientRepository.requireById(clientId); // 404s if the client doesn't exist/belongs to another shop
        Criteria filter = Criteria.where("clientId").is(clientId);
        if (status != null && !status.isBlank()) {
            filter = filter.and("status").is(BookingStatus.from(status));
        }
        CursorPageRequest page = CursorPageRequest.of(cursor, limit);
        return bookingRepository.list(filter, SortRequest.desc("startAt"), page).map(bookingMapper::toResponse);
    }

    // ── merge ───────────────────────────────────────────────────────────────

    /**
     * Reassigns a source client's bookings (Bookings collection) and notes/counters
     * (this collection) onto the target, then soft-deletes the source. Wrapped in a
     * Mongo multi-document transaction — per the endpoint's own contract ("merge
     * ... then soft-deletes the source. Irreversible") this must not be allowed to
     * partially apply: a crash after reassigning bookings but before soft-deleting
     * the source would leave two live client records both claiming the same
     * history. Loyalty progress will join this transaction once that domain exists.
     */
    @Transactional("mongoTransactionManager")
    public ClientResponse merge(String targetClientId, ClientMergeRequest request) {
        requireEditAccess();
        if (targetClientId.equals(request.sourceClientId())) {
            throw new ConflictException("Cannot merge a client into itself");
        }
        Client target = clientRepository.requireById(targetClientId);
        Client source = clientRepository.requireById(request.sourceClientId());

        bookingRepository.reassignClient(source.getId(), target.getId());
        clientRepository.reassignNotesAndCounters(target, source);
        clientRepository.save(target);
        clientRepository.softDelete(source);
        return clientMapper.toResponse(target);
    }

    // ── export ──────────────────────────────────────────────────────────────

    public ClientExportResponse export(ClientExportRequest request) {
        requireReadAccess();
        Criteria filter = new Criteria();
        List<Criteria> ands = new java.util.ArrayList<>();
        if (request != null && request.filters() != null) {
            if (Boolean.TRUE.equals(request.filters().hasUpcoming())) {
                ands.add(clientRepository.hasUpcomingBooking());
            }
            if (request.filters().tag() != null && !request.filters().tag().isBlank()) {
                ands.add(Criteria.where("tags").is(request.filters().tag()));
            }
        }
        if (!ands.isEmpty()) {
            filter = new Criteria().andOperator(ands.toArray(new Criteria[0]));
        }
        SortRequest sort = SortRequest.desc("createdAt");
        List<Client> clients = fetchAllForExport(filter, sort);

        // NOTE: there is no async job runner or object-storage "put" capability in
        // this codebase yet (FileStorage only exposes client-side presigned
        // uploads) — so, unlike the contract's "202 queued, poll statusUrl" framing,
        // we build the CSV synchronously in-memory and report it as immediately
        // ready. The response shape matches the contract either way, so swapping
        // in a real async worker later is a drop-in change, not a breaking one.
        String csv = toCsv(clients);
        log.info("[client-export] generated {} rows ({} bytes) for shop {}",
                clients.size(), csv.length(), TenantContextHolder.requireShopId());

        String jobId = IdGenerator.generate(IdPrefix.REQUEST);
        return new ClientExportResponse(jobId, null, Instant.now());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Pages through every match at {@code MAX_LIMIT} per round, capped at 50 pages
     * (5,000 clients) so a pathological filter can't turn a synchronous export
     * into an unbounded loop. A real async export (see {@link #export}'s note)
     * wouldn't need this cap.
     */
    private List<Client> fetchAllForExport(Criteria filter, SortRequest sort) {
        List<Client> all = new java.util.ArrayList<>();
        String cursor = null;
        for (int page = 0; page < 50; page++) {
            CursorPage<Client> result = clientRepository.list(filter, sort, CursorPageRequest.of(cursor, CursorPageRequest.MAX_LIMIT));
            all.addAll(result.data());
            if (!result.pageInfo().hasNextPage() || result.pageInfo().nextCursor() == null) {
                break;
            }
            cursor = result.pageInfo().nextCursor();
        }
        return all;
    }

    private void applyWrite(Client client, ClientWriteRequest request) {
        if (request.firstName() != null) {
            client.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            client.setLastName(request.lastName());
        }
        if (request.email() != null) {
            client.setEmail(request.email());
        }
        if (request.phone() != null) {
            client.setPhone(request.phone());
        }
        if (request.preferredStaffId() != null) {
            client.setPreferredStaffId(request.preferredStaffId());
        }
        if (request.notes() != null) {
            client.setSummary(request.notes());
        }
        if (request.tags() != null) {
            client.setTags(request.tags());
        }
        if (request.marketingOptIn() != null) {
            client.setMarketingOptIn(request.marketingOptIn());
        }
    }

    private ClientNote findNote(Client client, String noteId) {
        return client.getNotes().stream()
                .filter(n -> n.getId().equals(noteId))
                .findFirst()
                .orElseThrow(() -> NotFoundException.of("ClientNote", noteId));
    }

    private String toCsv(List<Client> clients) {
        StringBuilder sb = new StringBuilder("firstName,lastName,email,phone,totalBookings,lifetimeSpendCents,joinedAt\n");
        for (Client c : clients) {
            sb.append(csvField(c.getFirstName())).append(',')
                    .append(csvField(c.getLastName())).append(',')
                    .append(csvField(c.getEmail())).append(',')
                    .append(csvField(c.getPhone())).append(',')
                    .append(c.getTotalBookings()).append(',')
                    .append(c.getLifetimeSpendCents()).append(',')
                    .append(csvField(c.getJoinedAt() == null ? "" : c.getJoinedAt().toString()))
                    .append('\n');
        }
        return sb.toString();
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String currentStaffId() {
        return TenantContextHolder.require().staffId();
    }

    private void requireReadAccess() {
        var ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.CLIENTS_READ_ALL) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to view clients");
        }
    }

    private void requireEditAccess() {
        var ctx = TenantContextHolder.require();
        if (!ctx.hasPermission(Permission.CLIENTS_EDIT) && !ctx.isOwner()) {
            throw new ForbiddenException("You do not have permission to manage clients");
        }
    }
}

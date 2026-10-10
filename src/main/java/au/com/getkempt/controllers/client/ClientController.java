package au.com.getkempt.controllers.client;

import au.com.getkempt.common.pagination.CursorPage;
import au.com.getkempt.responses.booking.BookingResponse;
import au.com.getkempt.request.client.ClientExportRequest;
import au.com.getkempt.request.client.ClientMergeRequest;
import au.com.getkempt.request.client.ClientNoteUpdateRequest;
import au.com.getkempt.request.client.ClientNoteWriteRequest;
import au.com.getkempt.request.client.ClientWriteRequest;
import au.com.getkempt.responses.client.ClientExportResponse;
import au.com.getkempt.responses.client.ClientLoyaltyResponse;
import au.com.getkempt.responses.client.ClientNoteResponse;
import au.com.getkempt.responses.client.ClientResponse;
import au.com.getkempt.services.client.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** {@code /api/v1/clients} — see {@code paths/clients.yaml}. */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public CursorPage<ClientResponse> listClients(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String preferredStaffId,
            @RequestParam(required = false) Boolean hasUpcoming) {
        return clientService.list(cursor, limit, sort, q, tag, preferredStaffId, hasUpcoming);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse createClient(@Valid @RequestBody ClientWriteRequest request) {
        return clientService.create(request);
    }

    @GetMapping("/search")
    public List<ClientResponse> searchClients(
            @RequestParam String q,
            @RequestParam(required = false) Integer limit) {
        return clientService.search(q, limit);
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ClientExportResponse exportClients(@RequestBody(required = false) ClientExportRequest request) {
        return clientService.export(request);
    }

    @GetMapping("/{clientId}")
    public ClientResponse getClient(@PathVariable String clientId) {
        return clientService.get(clientId);
    }

    @PatchMapping("/{clientId}")
    public ClientResponse updateClient(
            @PathVariable String clientId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody ClientWriteRequest request) {
        return clientService.update(clientId, ifMatch, request);
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable String clientId) {
        clientService.delete(clientId);
    }

    @GetMapping("/{clientId}/bookings")
    public CursorPage<BookingResponse> listClientBookings(
            @PathVariable String clientId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String status) {
        return clientService.listBookings(clientId, cursor, limit, status);
    }

    @GetMapping("/{clientId}/notes")
    public List<ClientNoteResponse> listClientNotes(@PathVariable String clientId) {
        return clientService.listNotes(clientId);
    }

    @PostMapping("/{clientId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClientNoteResponse createClientNote(
            @PathVariable String clientId,
            @Valid @RequestBody ClientNoteWriteRequest request) {
        return clientService.createNote(clientId, request);
    }

    @PatchMapping("/{clientId}/notes/{noteId}")
    public ClientNoteResponse updateClientNote(
            @PathVariable String clientId,
            @PathVariable String noteId,
            @RequestBody ClientNoteUpdateRequest request) {
        return clientService.updateNote(clientId, noteId, request);
    }

    @DeleteMapping("/{clientId}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClientNote(@PathVariable String clientId, @PathVariable String noteId) {
        clientService.deleteNote(clientId, noteId);
    }

    @GetMapping("/{clientId}/loyalty")
    public ClientLoyaltyResponse getClientLoyalty(@PathVariable String clientId) {
        return clientService.getLoyalty(clientId);
    }

    @PostMapping("/{clientId}/merge")
    public ClientResponse mergeClient(
            @PathVariable String clientId,
            @Valid @RequestBody ClientMergeRequest request) {
        return clientService.merge(clientId, request);
    }
}

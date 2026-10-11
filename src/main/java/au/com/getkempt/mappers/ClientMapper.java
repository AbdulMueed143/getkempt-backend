package au.com.getkempt.mappers;

import au.com.getkempt.entities.Client;
import au.com.getkempt.entities.ClientNote;
import au.com.getkempt.responses.client.ClientNoteResponse;
import au.com.getkempt.responses.client.ClientResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientMapper {

    public ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail(),
                client.getPhone(),
                client.getTotalBookings(),
                client.getLifetimeSpendCents(),
                client.getLastBookingDate(),
                client.getNextBookingDate(),
                client.getPreferredStaffId(),
                client.getJoinedAt(),
                client.isMarketingOptIn(),
                client.isSmsOptIn(),
                client.isEmailOptIn(),
                client.getSummary(),
                nullSafe(client.getTags()),
                client.getCreatedAt(),
                client.getDeletedAt());
    }

    public ClientNoteResponse toNoteResponse(ClientNote note) {
        return new ClientNoteResponse(
                note.getId(),
                note.getBody(),
                note.isPinned(),
                note.getCreatedAt(),
                note.getCreatedByStaffId());
    }

    private static List<String> nullSafe(List<String> list) {
        return list == null ? List.of() : list;
    }
}

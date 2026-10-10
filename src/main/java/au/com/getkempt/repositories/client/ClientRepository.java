package au.com.getkempt.repositories.client;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.Client;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Tenant-scoped data access for {@link Client}. */
@Repository
public class ClientRepository extends TenantRepository<Client> {

    public ClientRepository(MongoTemplate template) {
        super(template, Client.class, IdPrefix.CLIENT);
    }

    /** Used to upsert-by-phone when a booking supplies an inline {@code client} payload. */
    public Optional<Client> findByPhone(String phone) {
        return Optional.ofNullable(template.findOne(scopedQuery(Criteria.where("phone").is(phone)), Client.class));
    }

    /**
     * Lightweight autocomplete search across name/email/phone, used by the booking
     * form. Deliberately un-paginated (cursor pagination would be overkill for a
     * type-ahead) — capped at {@code limit}.
     */
    public List<Client> search(String q, int limit) {
        Criteria match = nameEmailPhoneMatch(q);
        Query query = scopedQuery(match).limit(limit);
        return template.find(query, Client.class);
    }

    /** Builds the shared "search across name/email/phone" criteria for {@code ?q=}. */
    public Criteria nameEmailPhoneMatch(String q) {
        Pattern pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
        return new Criteria().orOperator(
                Criteria.where("firstName").regex(pattern),
                Criteria.where("lastName").regex(pattern),
                Criteria.where("email").regex(pattern),
                Criteria.where("phone").regex(pattern));
    }

    public Criteria hasUpcomingBooking() {
        return Criteria.where("nextBookingDate").gte(Instant.now());
    }

    // TenantRepository has no bulk-reassignment helper (merge is Clients-specific),
    // so this stays here rather than being promoted to the base class.
    public void reassignNotesAndCounters(Client target, Client source) {
        target.getNotes().addAll(source.getNotes());
        target.setTotalBookings(target.getTotalBookings() + source.getTotalBookings());
        target.setLifetimeSpendCents(target.getLifetimeSpendCents() + source.getLifetimeSpendCents());
        if (source.getLastBookingDate() != null
                && (target.getLastBookingDate() == null || source.getLastBookingDate().isAfter(target.getLastBookingDate()))) {
            target.setLastBookingDate(source.getLastBookingDate());
        }
        if (source.getNextBookingDate() != null
                && (target.getNextBookingDate() == null || source.getNextBookingDate().isBefore(target.getNextBookingDate()))) {
            target.setNextBookingDate(source.getNextBookingDate());
        }
        if (source.getJoinedAt() != null
                && (target.getJoinedAt() == null || source.getJoinedAt().isBefore(target.getJoinedAt()))) {
            target.setJoinedAt(source.getJoinedAt());
        }
    }
}

package au.com.getkempt.repositories.loyalty;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.loyalty.ClientLoyaltyState;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Tenant-scoped data access for {@link ClientLoyaltyState}; {@code id == clientId}. */
@Repository
public class ClientLoyaltyStateRepository extends TenantRepository<ClientLoyaltyState> {

    public ClientLoyaltyStateRepository(MongoTemplate template) {
        // The IdPrefix here is inert: getOrCreate() always pre-sets the id to the
        // client's own id before inserting, so TenantRepository.insert()'s
        // auto-generation branch (the only place idPrefix is consulted) never runs.
        super(template, ClientLoyaltyState.class, IdPrefix.CLIENT);
    }

    public ClientLoyaltyState getOrCreate(String clientId) {
        return findById(clientId).orElseGet(() -> {
            ClientLoyaltyState state = new ClientLoyaltyState();
            state.setId(clientId);
            state.setClientId(clientId);
            return insert(state);
        });
    }
}

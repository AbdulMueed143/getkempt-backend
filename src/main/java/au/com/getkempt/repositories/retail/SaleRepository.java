package au.com.getkempt.repositories.retail;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.retail.Sale;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Tenant-scoped data access for {@link Sale}. */
@Repository
public class SaleRepository extends TenantRepository<Sale> {

    public SaleRepository(MongoTemplate template) {
        super(template, Sale.class, IdPrefix.SALE);
    }
}

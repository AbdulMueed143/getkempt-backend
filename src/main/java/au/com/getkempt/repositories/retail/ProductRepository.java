package au.com.getkempt.repositories.retail;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.retail.Product;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Tenant-scoped data access for {@link Product}. */
@Repository
public class ProductRepository extends TenantRepository<Product> {

    public ProductRepository(MongoTemplate template) {
        super(template, Product.class, IdPrefix.PRODUCT);
    }

    public Optional<Product> findBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(template.findOne(scopedQuery(Criteria.where("sku").is(sku)), Product.class));
    }
}

package au.com.getkempt.repositories.service;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/** Tenant-scoped data access for {@link Service}. */
@Repository
public class ServiceRepository extends TenantRepository<Service> {

    public ServiceRepository(MongoTemplate template) {
        super(template, Service.class, IdPrefix.SERVICE);
    }

    public List<Service> findByIds(Collection<String> ids) {
        return template.find(scopedQuery(Criteria.where("_id").in(ids)), Service.class);
    }

    /** Highest current displayOrder, for appending new services to the end. */
    public int maxDisplayOrder() {
        Query q = scopedQuery(null).with(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "displayOrder")).limit(1);
        Service top = template.findOne(q, Service.class);
        return top == null ? -1 : top.getDisplayOrder();
    }

    /** True if any non-deleted package references the given service. */
    public boolean isReferencedByPackage(String serviceId) {
        Query q = new Query(Criteria.where("shopId").is(tenantShopId())
                .and("deletedAt").is(null)
                .and("serviceIds").is(serviceId));
        return template.exists(q, "packages");
    }

    private String tenantShopId() {
        return au.com.getkempt.common.tenant.TenantContextHolder.requireShopId();
    }
}

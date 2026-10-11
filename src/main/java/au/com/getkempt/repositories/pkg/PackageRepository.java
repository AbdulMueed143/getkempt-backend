package au.com.getkempt.repositories.pkg;

import au.com.getkempt.common.id.IdPrefix;
import au.com.getkempt.common.persistence.TenantRepository;
import au.com.getkempt.entities.Package;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Tenant-scoped data access for {@link Package}. */
@Repository
public class PackageRepository extends TenantRepository<Package> {

    public PackageRepository(MongoTemplate template) {
        super(template, Package.class, IdPrefix.PACKAGE);
    }
}

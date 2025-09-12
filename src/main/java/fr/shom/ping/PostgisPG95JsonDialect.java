package fr.shom.ping;

import java.io.Serial;
import java.io.Serializable;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.locationtech.jts.geom.Geometry;

public class PostgisPG95JsonDialect extends PostgreSQLDialect implements Serializable {
    @Serial
    private static final long serialVersionUID = -318362902614752210L;

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
        ddlTypeRegistry.addDescriptor(new DdlTypeImpl(2003, Geometry.class.getName(), this));
        super.contributeTypes(typeContributions, serviceRegistry);
    }
}

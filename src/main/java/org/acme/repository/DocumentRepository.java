package org.acme.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.model.entity.DocumentFile;

@ApplicationScoped
public class DocumentRepository
        implements PanacheRepository<DocumentFile> {
}

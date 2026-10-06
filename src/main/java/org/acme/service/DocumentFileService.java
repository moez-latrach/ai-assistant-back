package org.acme.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.model.dto.DocumentResponse;
import org.acme.model.dto.UploadResponse;
import org.acme.model.entity.DocumentFile;
import org.acme.repository.DocumentRepository;
import org.acme.service.constants.LogMessages;
import org.acme.service.constants.RagConstants;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@ApplicationScoped
public class DocumentFileService {

    @Inject
    Instance<DocumentExtractor> extractors;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    private static final Logger LOG = Logger.getLogger(DocumentFileService.class);

    /* SPLITTER */

    private final DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);

    /* UPLOAD */

    @Transactional
    public UploadResponse upload(FileUpload file) throws Exception {

        /* SAVE DOCUMENT */

        DocumentFile document = new DocumentFile();

        document.setFilename(file.fileName());

        document.setUploadedAt(LocalDateTime.now());

        documentRepository.persist(document);

        /* EXTRACT TEXT */
        try (InputStream inputStream = Files.newInputStream(file.uploadedFile())) {
            String text = extract(file.fileName(), inputStream);
            ingest(text, document.getId(), document.getFilename());
        }
        LOG.info(LogMessages.INGESTION_COMPLETED);

        return new UploadResponse(
                document.getId(),
                document.getFilename()
        );
    }

    /* EXTRACT */

    private String extract(String fileName, InputStream inputStream) {

        return extractors.stream().filter(e -> e.supports(fileName)).findFirst().orElseThrow(() -> new RuntimeException("Format non supporté")).extract(inputStream);
    }

    private void ingest(String text, UUID documentId, String filename) {

        Document parsedDocument = Document.from(text);
        List<TextSegment> segments = splitter.split(parsedDocument);
        LOG.infof(LogMessages.CHUNK_COUNT, segments.size());

        for (int i = 0; i < segments.size(); i++) {

            TextSegment segment = segments.get(i);
            Metadata metadata = new Metadata();
            metadata.put(RagConstants.DOCUMENT_ID, documentId.toString());
            metadata.put(RagConstants.FILE_NAME, filename);
            metadata.put(RagConstants.CHUNK_INDEX, i);
            TextSegment enrichedSegment = TextSegment.from(segment.text(), metadata);
            LOG.infof(LogMessages.CHUNK_DETAIL, i, filename, segment.text().length(), segment.text());
            embeddingStore.add(embeddingModel.embed(enrichedSegment).content(), enrichedSegment);

        }
    }

    public List<DocumentResponse> findAll() {
        return documentRepository.listAll().stream()

                .map(document ->
                        new DocumentResponse(document.getId(), document.getFilename()))
                .toList();
    }

}

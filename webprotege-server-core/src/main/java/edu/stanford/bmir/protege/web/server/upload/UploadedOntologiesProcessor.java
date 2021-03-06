package edu.stanford.bmir.protege.web.server.upload;

import edu.stanford.bmir.protege.web.server.inject.UploadsDirectory;
import edu.stanford.bmir.protege.web.server.owlapi.WebProtegeOWLManager;
import edu.stanford.bmir.protege.web.server.project.Ontology;
import edu.stanford.bmir.protege.web.server.project.RawProjectSourcesImporter;
import edu.stanford.bmir.protege.web.server.project.UploadedProjectSourcesExtractor;
import edu.stanford.bmir.protege.web.shared.csv.DocumentId;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-20
 *
 * Processes an uploaded file as an ontology document or zip of ontology documents.
 */
public class UploadedOntologiesProcessor {

    @Nonnull
    private final File uploadsDirectory;

    @Nonnull
    private final Provider<UploadedProjectSourcesExtractor> uploadedProjectSourcesExtractorProvider;

    @Inject
    public UploadedOntologiesProcessor(@Nonnull @UploadsDirectory File uploadsDirectory,
                                       @Nonnull Provider<UploadedProjectSourcesExtractor> uploadedProjectSourcesExtractorProvider) {
        this.uploadsDirectory = checkNotNull(uploadsDirectory);
        this.uploadedProjectSourcesExtractorProvider = uploadedProjectSourcesExtractorProvider;
    }

    @Nonnull
    public Collection<Ontology> getUploadedOntologies(@Nonnull DocumentId documentId) throws OWLOntologyCreationException, IOException {
        return loadOntologies(documentId);
    }

    private Collection<Ontology> loadOntologies(@Nonnull DocumentId documentId) throws IOException, OWLOntologyCreationException {
        var manager = WebProtegeOWLManager.createOWLOntologyManager();
        var uploadedFile = new File(uploadsDirectory, documentId.getDocumentId());
        var uploadedProjectSourcesExtractor = uploadedProjectSourcesExtractorProvider.get();
        var rawProjectSources = uploadedProjectSourcesExtractor.extractProjectSources(uploadedFile);
        var loaderConfig = new OWLOntologyLoaderConfiguration();
        var rawProjectSourcesImporter = new RawProjectSourcesImporter(manager, loaderConfig);
        rawProjectSourcesImporter.importRawProjectSources(rawProjectSources);
        return manager.getOntologies()
                      .stream()
                      .map(this::toOntology)
                      .collect(toList());
    }

    private Ontology toOntology(OWLOntology ont) {
        return Ontology.get(ont.getOntologyID(),
                            ont.getAnnotations(),
                            ont.getAxioms());
    }

}

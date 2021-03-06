package edu.stanford.bmir.protege.web.server.index;

import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-15
 */
public interface ProjectSignatureIndex {

    /**
     * Returns a stream of entities that are in the signature of the project ontologies.
     * @return A stream of entities.  This may contain duplicate entities.
     */
    @Nonnull
    Stream<OWLEntity> getSignature();
}

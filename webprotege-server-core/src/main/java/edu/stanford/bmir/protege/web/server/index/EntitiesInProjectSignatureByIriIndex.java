package edu.stanford.bmir.protege.web.server.index;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-13
 */
public interface EntitiesInProjectSignatureByIriIndex {

    /**
     * Gets the entities that are contained in the signature of project ontologies
     * that have the specified entity IRI.
     * @param entityIri the entity IRI
     * @return A stream of distinct entities that are contained in the signature of
     * the project ontologies.
     */
    @Nonnull
    Stream<OWLEntity> getEntityInSignature(@Nonnull IRI entityIri);

}

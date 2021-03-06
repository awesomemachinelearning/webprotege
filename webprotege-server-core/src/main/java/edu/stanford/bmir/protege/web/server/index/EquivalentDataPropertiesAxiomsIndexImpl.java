package edu.stanford.bmir.protege.web.server.index;

import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLOntologyID;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-24
 */
public class EquivalentDataPropertiesAxiomsIndexImpl implements EquivalentDataPropertiesAxiomsIndex {

    @Nonnull
    private final OntologyIndex ontologyIndex;

    @Inject
    public EquivalentDataPropertiesAxiomsIndexImpl(@Nonnull OntologyIndex ontologyIndex) {
        this.ontologyIndex = checkNotNull(ontologyIndex);
    }

    @Nonnull
    @Override
    public Stream<OWLEquivalentDataPropertiesAxiom> getEquivalentDataPropertiesAxioms(@Nonnull OWLDataProperty property,
                                                                                      @Nonnull OWLOntologyID ontologyId) {
        checkNotNull(ontologyId);
        checkNotNull(property);
        return ontologyIndex.getOntology(ontologyId)
                .stream()
                .flatMap(ont -> ont.getEquivalentDataPropertiesAxioms(property).stream());
    }
}

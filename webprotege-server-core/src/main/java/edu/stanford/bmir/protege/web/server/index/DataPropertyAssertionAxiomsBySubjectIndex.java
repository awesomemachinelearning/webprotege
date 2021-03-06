package edu.stanford.bmir.protege.web.server.index;

import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntologyID;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-12
 */
public interface DataPropertyAssertionAxiomsBySubjectIndex {

    @Nonnull
    Stream<OWLDataPropertyAssertionAxiom> getDataPropertyAssertions(@Nonnull
                                                                         OWLIndividual individual,
                                                                    @Nonnull
                                                                         OWLOntologyID ontologyId);
}

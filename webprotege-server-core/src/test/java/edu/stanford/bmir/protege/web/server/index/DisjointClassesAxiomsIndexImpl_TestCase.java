package edu.stanford.bmir.protege.web.server.index;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

import java.util.Collections;
import java.util.Optional;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2019-08-22
 */
@RunWith(MockitoJUnitRunner.class)
public class DisjointClassesAxiomsIndexImpl_TestCase {

    private DisjointClassesAxiomsIndexImpl impl;

    @Mock
    private OntologyIndex ontologyIndex;

    @Mock
    private OWLOntology ontology;

    @Mock
    private OWLOntologyID ontologyID;

    @Mock
    private OWLClass cls;

    @Mock
    private OWLDisjointClassesAxiom axiom;

    @Before
    public void setUp() {
        when(ontologyIndex.getOntology(any()))
                .thenReturn(Optional.empty());
        when(ontologyIndex.getOntology(ontologyID))
                .thenReturn(Optional.of(ontology));
        when(ontology.getDisjointClassesAxioms(cls))
                .thenReturn(Collections.singleton(axiom));
        impl = new DisjointClassesAxiomsIndexImpl(ontologyIndex);
    }

    @Test
    public void shouldGetDisjointClassesAxiomForClass() {
        var axioms = impl.getDisjointClassesAxioms(cls, ontologyID).collect(toSet());
        assertThat(axioms, hasItem(axiom));
    }

    @Test
    public void shouldGetEmptySetForUnknownOntologyId() {
        var axioms = impl.getDisjointClassesAxioms(cls, mock(OWLOntologyID.class)).collect(toSet());
        assertThat(axioms.isEmpty(), is(true));
    }

    @Test
    public void shouldGetEmptySetForUnknownClass() {
        var axioms = impl.getDisjointClassesAxioms(mock(OWLClass.class), ontologyID).collect(toSet());
        assertThat(axioms.isEmpty(), is(true));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void shouldThrowNpeForNullOntologyId() {
        impl.getDisjointClassesAxioms(cls, null);
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void shouldThrowNpeForNullClass() {
        impl.getDisjointClassesAxioms(null, ontologyID);
    }

}

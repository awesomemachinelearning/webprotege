package edu.stanford.bmir.protege.web.server.crud.supplied;

import edu.stanford.bmir.protege.web.server.change.OntologyChangeFactory;
import edu.stanford.bmir.protege.web.server.change.OntologyChangeFactoryImpl;
import edu.stanford.bmir.protege.web.server.change.OntologyChangeList;
import edu.stanford.bmir.protege.web.server.crud.ChangeSetEntityCrudSession;
import edu.stanford.bmir.protege.web.server.crud.EntityCrudContext;
import edu.stanford.bmir.protege.web.server.crud.PrefixedNameExpander;
import edu.stanford.bmir.protege.web.server.index.OntologyIndex;
import edu.stanford.bmir.protege.web.shared.crud.EntityCrudKitPrefixSettings;
import edu.stanford.bmir.protege.web.shared.crud.EntityShortForm;
import edu.stanford.bmir.protege.web.shared.crud.supplied.SuppliedNameSuffixSettings;
import edu.stanford.bmir.protege.web.shared.crud.supplied.WhiteSpaceTreatment;
import edu.stanford.bmir.protege.web.shared.shortform.DictionaryLanguage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.Namespaces;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.bmir.protege.web.server.OWLDeclarationAxiomMatcher.declarationFor;
import static edu.stanford.bmir.protege.web.server.OWLEntityMatcher.owlThing;
import static edu.stanford.bmir.protege.web.server.RdfsLabelWithLexicalValueAndLang.rdfsLabelWithLexicalValueAndLang;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 16/04/2014
 */
@RunWith(MockitoJUnitRunner.class)
public class SuppliedNameSuffixEntityCrudKitHandlerTestCase {

    public static final String PREFIX = "http://stuff/";
    @Mock
    protected EntityCrudKitPrefixSettings prefixSettings;

    @Mock
    protected SuppliedNameSuffixSettings suffixSettings;

    @Mock
    protected EntityCrudContext crudContext;

    @Mock
    protected EntityShortForm entityShortForm;

    @Mock
    protected OWLOntology ontology;

    @Mock
    protected OntologyChangeList.Builder<OWLClass> builder;

    @Mock
    protected ChangeSetEntityCrudSession session;

    protected WhiteSpaceTreatment whiteSpaceTreatment = WhiteSpaceTreatment.TRANSFORM_TO_CAMEL_CASE;

    private SuppliedNameSuffixEntityCrudKitHandler handler;

    @Mock
    private DictionaryLanguage dictionaryLanguage;

    @Mock
    private OWLOntologyID ontologyId;

    private OntologyChangeFactory changeFactory;

    @Mock
    private OntologyIndex ontologyIndex;


    @Before
    public void setUp() throws Exception {
        OWLDataFactoryImpl dataFactory = new OWLDataFactoryImpl();
        when(prefixSettings.getIRIPrefix()).thenReturn(PREFIX);
        when(suffixSettings.getWhiteSpaceTreatment()).thenReturn(whiteSpaceTreatment);
        when(crudContext.getTargetOntologyId()).thenReturn(ontologyId);
        when(crudContext.getPrefixedNameExpander()).thenReturn(PrefixedNameExpander.builder().withNamespaces(Namespaces.values()).build());
        when(crudContext.getDictionaryLanguage()).thenReturn(dictionaryLanguage);
        when(ontology.containsEntityInSignature(any(OWLEntity.class))).thenReturn(true);
        when(dictionaryLanguage.getLang()).thenReturn("");
        when(ontologyIndex.getOntology(ontologyId))
                .thenReturn(Optional.of(ontology));
        changeFactory = new OntologyChangeFactoryImpl(ontologyIndex);
        handler = new SuppliedNameSuffixEntityCrudKitHandler(prefixSettings, suffixSettings, dataFactory, changeFactory);
    }

    @Test
    public void shouldAddDeclaration() {
        when(entityShortForm.getShortForm()).thenReturn("A");
        OWLClass cls = handler.create(session, EntityType.CLASS, entityShortForm, Optional.empty(), crudContext, builder);
        var ontologyChangeCaptor = ArgumentCaptor.forClass(OWLOntologyChange.class);
        verify(builder, atLeast(1)).add(ontologyChangeCaptor.capture());
        var addedAxioms = ontologyChangeCaptor.getAllValues()
                .stream()
                .map(OWLOntologyChange::getAxiom)
                .filter(ax -> ax instanceof OWLDeclarationAxiom)
                .map(ax -> (OWLDeclarationAxiom) ax)
                .collect(Collectors.toList());
        assertThat(addedAxioms, hasItem(is(declarationFor(cls))));
    }

    @Test
    public void shouldAddLabelEqualToSuppliedName() {
        String suppliedName = "MyLabel";
        when(entityShortForm.getShortForm()).thenReturn(suppliedName);
        handler.create(session, EntityType.CLASS, entityShortForm, Optional.of("en"), crudContext, builder);
        verifyHasLabelEqualTo(suppliedName, "en");
    }

    @Test
    public void shouldCreatedExpandedPrefixName() {
        when(entityShortForm.getShortForm()).thenReturn("owl:Thing");
        OWLClass cls = handler.create(session, EntityType.CLASS, entityShortForm, Optional.of("en"), crudContext, builder);
        assertThat(cls, is(owlThing()));
    }

    @Test
    public void shouldAddLabelEqualToLocalName() {
        String suppliedName = "owl:Thing";
        when(entityShortForm.getShortForm()).thenReturn(suppliedName);
        handler.create(session, EntityType.CLASS, entityShortForm, Optional.of("en"), crudContext, builder);
        verifyHasLabelEqualTo("Thing", "en");
    }

    @Test
    public void shouldCreateEntityWithAbsoluteIriIfSpecified() {
        String expectedIRI = "http://stuff.com/A";
        String shortForm = "<" + expectedIRI + ">";
        when(entityShortForm.getShortForm()).thenReturn(shortForm);
        OWLClass cls = handler.create(session, EntityType.CLASS, entityShortForm, Optional.of("en"), crudContext, builder);
        assertThat(cls.getIRI(), is(equalTo(IRI.create(expectedIRI))));
        verifyHasLabelEqualTo("A", "en");
    }


    private void verifyHasLabelEqualTo(String label, String lang) {
        var addAxiomCaptor = ArgumentCaptor.forClass(OWLOntologyChange.class);
        verify(builder, atLeast(1)).add(addAxiomCaptor.capture());
        List<OWLAxiom> addedAxioms = addAxiomCaptor.getAllValues().stream()
                                                                      .map(OWLOntologyChange::getAxiom)
                .collect(Collectors.toList());
        assertThat(addedAxioms, hasItem(rdfsLabelWithLexicalValueAndLang(label, lang)));
    }
}

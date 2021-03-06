package edu.stanford.bmir.protege.web.server.revision;

import com.google.common.collect.ImmutableList;
import edu.stanford.bmir.protege.web.server.axiom.*;
import edu.stanford.bmir.protege.web.server.change.ChangeRecordComparator;
import edu.stanford.bmir.protege.web.server.diff.Revision2DiffElementsTranslator;
import edu.stanford.bmir.protege.web.server.index.*;
import edu.stanford.bmir.protege.web.server.lang.ActiveLanguagesManagerImpl;
import edu.stanford.bmir.protege.web.server.lang.LanguageManager;
import edu.stanford.bmir.protege.web.server.mansyntax.render.*;
import edu.stanford.bmir.protege.web.server.object.OWLObjectComparatorImpl;
import edu.stanford.bmir.protege.web.server.owlapi.ProjectAnnotationAssertionAxiomsBySubjectIndexImpl;
import edu.stanford.bmir.protege.web.server.project.DefaultOntologyIdManager;
import edu.stanford.bmir.protege.web.server.project.ProjectDetailsRepository;
import edu.stanford.bmir.protege.web.server.renderer.OWLObjectRendererImpl;
import edu.stanford.bmir.protege.web.server.renderer.RenderingManager;
import edu.stanford.bmir.protege.web.server.renderer.ShortFormAdapter;
import edu.stanford.bmir.protege.web.server.shortform.*;
import edu.stanford.bmir.protege.web.shared.change.ProjectChange;
import edu.stanford.bmir.protege.web.shared.object.*;
import edu.stanford.bmir.protege.web.shared.pagination.Page;
import edu.stanford.bmir.protege.web.shared.pagination.PageRequest;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLEntityComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 15 Mar 2017
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectChangesManager_IT {

    public static final int CHANGE_COUNT = 100;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File changeHistoryFile;

    private ProjectChangesManager changesManager;

    private ProjectId projectId = ProjectId.get(UUID.randomUUID().toString());

    @Mock
    private ProjectDetailsRepository repo;

    @Mock
    private DefaultOntologyIdManager defaultOntologyIdManager;

    @Mock
    private OWLOntologyID ontologyId;

    @Before
    public void setUp() throws Exception {
        changeHistoryFile = temporaryFolder.newFile();
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology rootOntology = manager.createOntology(IRI.create("http://stuff.com/ont"));
        OWLDataFactory dataFactory = manager.getOWLDataFactory();
        RevisionManager revisionManager = new RevisionManagerImpl(new RevisionStoreImpl(
                projectId,
                changeHistoryFile,
                dataFactory
        ));
        when(defaultOntologyIdManager.getDefaultOntologyId())
                .thenReturn(rootOntology.getOntologyID());
        when(repo.findOne(projectId)).thenReturn(Optional.empty());
        when(repo.getDisplayNameLanguages(projectId)).thenReturn(ImmutableList.of());
        var ontologiesIndex = new ProjectOntologiesIndexImpl(rootOntology);
        var ontologyIndex = new OntologyIndexImpl(rootOntology);

        var annotationAssertionsIndex = new AnnotationAssertionAxiomsBySubjectIndexImpl(ontologyIndex);
        var projectAnnotationAssertionsIndex = new ProjectAnnotationAssertionAxiomsBySubjectIndexImpl(ontologiesIndex,
                                                                                                      annotationAssertionsIndex);
        var annotationAssertionAxioms = new ProjectAnnotationAssertionAxiomsBySubjectIndexImpl(ontologiesIndex, annotationAssertionsIndex);
        AxiomsByTypeIndex axiomsByTypeIndex = new AxiomsByTypeIndexImpl(ontologyIndex);

        AnnotationAssertionAxiomsIndex assertionAxiomsIndex = new AnnotationAssertionAxiomsIndexWrapperImpl(ontologiesIndex,
                                                                                                            axiomsByTypeIndex,
                                                                                                            projectAnnotationAssertionsIndex);
        LanguageManager languageManager = new LanguageManager(projectId, new ActiveLanguagesManagerImpl(projectId,
                                                                                                        assertionAxiomsIndex), repo);
        var projectOntologiesIndex = new ProjectOntologiesIndexImpl(rootOntology);
        var entitiesInSignatureIndex = new EntitiesInProjectSignatureByIriIndexImpl(projectOntologiesIndex, ontologyIndex);
        var ontologySignatureIndex = new OntologySignatureIndexImpl(ontologyIndex);
        var projectSignatureIndex = new ProjectSignatureIndexImpl(projectOntologiesIndex, ontologySignatureIndex);

        var multilingualDictionary = new MultiLingualDictionaryImpl(projectId, new DictionaryBuilder(projectId, projectOntologiesIndex, axiomsByTypeIndex, entitiesInSignatureIndex, projectSignatureIndex), new DictionaryUpdater(projectAnnotationAssertionsIndex));
        DictionaryManager dictionaryManager = new DictionaryManager(languageManager,
                                                                    multilingualDictionary,
                                                                    new BuiltInShortFormDictionary(new ShortFormCache(),
                                                                                                   dataFactory));
        ShortFormAdapter shortFormAdapter = new ShortFormAdapter(dictionaryManager);
        OWLEntityComparator entityComparator = new OWLEntityComparator(
                shortFormAdapter
        );
        OWLClassExpressionSelector classExpressionSelector = new OWLClassExpressionSelector(entityComparator);
        OWLObjectPropertyExpressionSelector objectPropertyExpressionSelector = new OWLObjectPropertyExpressionSelector(
                entityComparator);
        OWLDataPropertyExpressionSelector dataPropertyExpressionSelector = new OWLDataPropertyExpressionSelector(
                entityComparator);
        OWLIndividualSelector individualSelector = new OWLIndividualSelector(entityComparator);
        SWRLAtomSelector atomSelector = new SWRLAtomSelector((o1, o2) -> 0);
        RenderingManager renderingManager = new RenderingManager(
                new DictionaryManager(languageManager, new MultiLingualDictionaryImpl(projectId, new DictionaryBuilder(projectId, projectOntologiesIndex, axiomsByTypeIndex, entitiesInSignatureIndex, projectSignatureIndex), new DictionaryUpdater(annotationAssertionAxioms)),
                                      new BuiltInShortFormDictionary(new ShortFormCache(), dataFactory)),
                NullDeprecatedEntityChecker.get(),
                new ManchesterSyntaxObjectRenderer(
                        shortFormAdapter,
                        new EntityIRICheckerImpl(entitiesInSignatureIndex),
                        LiteralStyle.BRACKETED,
                        new DefaultHttpLinkRenderer(),
                        new MarkdownLiteralRenderer()
                ));

        AxiomComparatorImpl axiomComparator = new AxiomComparatorImpl(
                new AxiomBySubjectComparator(
                        new AxiomSubjectProvider(
                                classExpressionSelector,
                                objectPropertyExpressionSelector,
                                dataPropertyExpressionSelector,
                                individualSelector,
                                atomSelector
                        ),
                        new OWLObjectComparatorImpl(renderingManager)
                ),
                new AxiomByTypeComparator(DefaultAxiomTypeOrdering.get()),
                new AxiomByRenderingComparator(new OWLObjectRendererImpl(renderingManager))
        );
        changesManager = new ProjectChangesManager(projectId, revisionManager,
                                                   renderingManager,
                                                   new ChangeRecordComparator(
                        axiomComparator,
                        (o1, o2) -> 0
                ),
                                                   () -> new Revision2DiffElementsTranslator(new WebProtegeOntologyIRIShortFormProvider(defaultOntologyIdManager),
                                                                                             defaultOntologyIdManager,
                                                                                             projectOntologiesIndex));


        createChanges(manager, rootOntology, dataFactory, revisionManager);
    }

    private static void createChanges(OWLOntologyManager manager,
                       OWLOntology rootOntology,
                       OWLDataFactory dataFactory,
                       RevisionManager revisionManager) {
        PrefixManager pm = new DefaultPrefixManager();
        pm.setDefaultPrefix("http://stuff.com/" );
        List<OWLOntologyChange> changeList = new ArrayList<>();
        for (int i = 0; i < CHANGE_COUNT; i++) {
            OWLClass clsA = dataFactory.getOWLClass(":A" + i, pm);
            OWLClass clsB = dataFactory.getOWLClass(":B" + i, pm);

            OWLSubClassOfAxiom ax = dataFactory.getOWLSubClassOfAxiom(clsA, clsB);
            changeList.add(new AddAxiom(rootOntology, ax));

            OWLAnnotationProperty rdfsLabel = dataFactory.getRDFSLabel();
            IRI annotationSubject = clsA.getIRI();
            OWLAnnotationAssertionAxiom labAxA = dataFactory.getOWLAnnotationAssertionAxiom(
                    rdfsLabel,
                    annotationSubject,
                    dataFactory.getOWLLiteral("Class A " + i)
            );
            changeList.add(new AddAxiom(rootOntology, labAxA));

            OWLAnnotationAssertionAxiom labAxB = dataFactory.getOWLAnnotationAssertionAxiom(
                    rdfsLabel,
                    annotationSubject,
                    dataFactory.getOWLLiteral("Class B " + i)
            );
            changeList.add(new AddAxiom(rootOntology, labAxB));
        }
        manager.applyChanges(changeList);
        revisionManager.addRevision(UserId.getUserId("MH" ),
                                    changeList.stream().map(OWLOntologyChange::getChangeRecord).collect(toList()),
                                    "Adding axioms");
    }

    @Test
    public void shouldSaveChangesToFile() {
        assertThat(changeHistoryFile.length(), is(greaterThan(0L)));
    }

    @Test
    public void shouldGetChanges() {
        Page<ProjectChange> projectChanges = changesManager.getProjectChanges(Optional.empty(),
                                                                              PageRequest.requestFirstPage());
        assertThat(projectChanges.getPageElements().size(), is(1));
    }

    @Test
    public void shouldContainOntologyChanges() {
        Page<ProjectChange> projectChanges = changesManager.getProjectChanges(Optional.empty(),
                                                                                       PageRequest.requestFirstPage());
        assertThat(projectChanges.getPageElements().get(0).getChangeCount(), is(3 * CHANGE_COUNT));
    }
}

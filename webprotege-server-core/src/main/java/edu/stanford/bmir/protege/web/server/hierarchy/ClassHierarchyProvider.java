package edu.stanford.bmir.protege.web.server.hierarchy;

import com.google.common.base.Stopwatch;
import edu.stanford.bmir.protege.web.server.index.*;
import edu.stanford.bmir.protege.web.shared.inject.ProjectSingleton;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.protege.owlapi.inference.cls.ChildClassExtractor;
import org.protege.owlapi.inference.cls.ParentClassExtractor;
import org.protege.owlapi.inference.orphan.TerminalElementFinder;
import org.semanticweb.owlapi.change.AxiomChangeData;
import org.semanticweb.owlapi.change.OWLOntologyChangeRecord;
import org.semanticweb.owlapi.change.RemoveAxiomData;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 17-Jan-2007<br><br>
 */
@ProjectSingleton
public class ClassHierarchyProvider extends AbstractHierarchyProvider<OWLClass> {

    private static final Logger logger = LoggerFactory.getLogger(ClassHierarchyProvider.class);

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final OWLClass root;

    @Nonnull
    private final TerminalElementFinder<OWLClass> rootFinder;

    @Nonnull
    private final Set<OWLClass> nodesToUpdate = new HashSet<>();

    @Nonnull
    private final ProjectOntologiesIndex projectOntologiesIndex;

    @Nonnull
    private final SubClassOfAxiomsBySubClassIndex subClassOfAxiomsIndex;

    @Nonnull
    private final EquivalentClassesAxiomsIndex equivalentClassesAxiomsIndex;

    @Nonnull
    private final ProjectSignatureByTypeIndex projectSignatureByTypeIndex;

    @Nonnull
    private final AxiomsByEntityReferenceIndex axiomsByEntityReferenceIndex;

    @Nonnull
    private final EntitiesInProjectSignatureByIriIndex entitiesInProjectSignatureByIriIndex;

    @Inject
    public ClassHierarchyProvider(ProjectId projectId,
                                  @Nonnull @ClassHierarchyRoot OWLClass rootCls,
                                  @Nonnull ProjectOntologiesIndex projectOntologiesIndex,
                                  @Nonnull SubClassOfAxiomsBySubClassIndex subClassOfAxiomsIndex,
                                  @Nonnull EquivalentClassesAxiomsIndex equivalentClassesAxiomsIndex,
                                  @Nonnull ProjectSignatureByTypeIndex projectSignatureByTypeIndex,
                                  @Nonnull AxiomsByEntityReferenceIndex axiomsByEntityReferenceIndex,
                                  @Nonnull EntitiesInProjectSignatureByIriIndex entitiesInProjectSignatureByIriIndex) {
        this.projectId = checkNotNull(projectId);
        this.root = checkNotNull(rootCls);
        this.projectOntologiesIndex = projectOntologiesIndex;
        this.subClassOfAxiomsIndex = subClassOfAxiomsIndex;
        this.equivalentClassesAxiomsIndex = equivalentClassesAxiomsIndex;
        this.projectSignatureByTypeIndex = projectSignatureByTypeIndex;
        this.axiomsByEntityReferenceIndex = axiomsByEntityReferenceIndex;
        this.entitiesInProjectSignatureByIriIndex = entitiesInProjectSignatureByIriIndex;
        rootFinder = new TerminalElementFinder<>(cls -> {
            Collection<OWLClass> parents = getParents(cls);
            parents.remove(root);
            return parents;
        });
        nodesToUpdate.clear();
        rebuildImplicitRoots();
        fireHierarchyChanged();
    }

    public Set<OWLClass> getParents(OWLClass object) {
        // If the object is thing then there are no
        // parents
        if(object.equals(root)) {
            return Collections.emptySet();
        }

        var subClassOfAxioms =
                projectOntologiesIndex.getOntologyIds()
                                      .flatMap(ontId -> subClassOfAxiomsIndex.getSubClassOfAxiomsForSubClass(object,
                                                                                                             ontId));

        var equivalentClassesAxioms =
                projectOntologiesIndex.getOntologyIds()
                                      .flatMap(ontId -> equivalentClassesAxiomsIndex.getEquivalentClassesAxioms(
                                              object,
                                              ontId));

        var axioms = Stream.concat(subClassOfAxioms, equivalentClassesAxioms);
        var parents = axioms.flatMap(ax -> extractParents(object, ax))
                            .collect(toSet());
        // Thing if the object is a root class
        if(rootFinder.getTerminalElements()
                     .contains(object)) {
            parents.add(root);
        }
        return parents;
    }

    private void rebuildImplicitRoots() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        logger.info("{} Rebuilding class hierarchy", projectId);
        rootFinder.clear();
        var signature = projectSignatureByTypeIndex.getSignature(EntityType.CLASS)
                                                   .collect(toImmutableSet());
        rootFinder.appendTerminalElements(signature);
        rootFinder.finish();
        logger.info("{} Rebuilt class hierarchy in {} ms", projectId, stopwatch.elapsed(MILLISECONDS));
    }

    private static Stream<OWLClass> extractParents(OWLClass object, OWLAxiom ax) {
        var parentClassExtractor = new ParentClassExtractor();
        parentClassExtractor.setCurrentClass(object);
        ax.accept(parentClassExtractor);
        return parentClassExtractor.getResult()
                                   .stream();
    }

    public void dispose() {
    }

    public void handleChanges(@Nonnull List<OWLOntologyChangeRecord> changes) {
        Set<OWLClass> oldTerminalElements = new HashSet<>(rootFinder.getTerminalElements());
        Set<OWLClass> changedClasses = new HashSet<>();
        changedClasses.add(root);
        List<OWLOntologyChangeRecord> filteredChanges = filterIrrelevantChanges(changes);
        updateImplicitRoots(filteredChanges);
        for(OWLOntologyChangeRecord change : filteredChanges) {
            changedClasses.addAll(change.getData()
                                        .getSignature()
                                        .stream()
                                        .filter(OWLEntity::isOWLClass)
                                        .filter(entity -> !entity.equals(root))
                                        .map(entity -> (OWLClass) entity)
                                        .collect(toList()));
        }
        changedClasses.forEach(this::registerNodeChanged);
        rootFinder.getTerminalElements()
                  .stream()
                  .filter(cls -> !oldTerminalElements.contains(cls))
                  .forEach(this::registerNodeChanged);
        oldTerminalElements.stream()
                           .filter(cls -> !rootFinder.getTerminalElements()
                                                     .contains(cls))
                           .forEach(this::registerNodeChanged);
        notifyNodeChanges();
    }

    private List<OWLOntologyChangeRecord> filterIrrelevantChanges(List<OWLOntologyChangeRecord> changes) {
        return changes.stream()
                      .filter(ClassHierarchyProvider::isAxiomChange)
                      .collect(toList());
    }

    private void updateImplicitRoots(List<OWLOntologyChangeRecord> changes) {
        Set<OWLClass> possibleTerminalElements = new HashSet<>();
        Set<OWLClass> notInOntologies = new HashSet<>();

        // only listen for changes on the appropriate ontologies
        changes.stream()
               .filter(ClassHierarchyProvider::isAxiomChange)
               .forEach(change -> {
                   boolean remove = change.getData() instanceof RemoveAxiomData;
                   var axiom = ((AxiomChangeData) change.getData()).getItem();
                   axiom.getSignature()
                        .stream()
                        .filter(OWLEntity::isOWLClass)
                        .filter(entity -> !entity.equals(root))
                        .forEach(entity -> {
                            OWLClass cls = (OWLClass) entity;
                            if(!remove || containsReference(cls)) {
                                possibleTerminalElements.add(cls);
                            }
                            else {
                                notInOntologies.add(cls);
                            }
                        });
               });

        possibleTerminalElements.addAll(rootFinder.getTerminalElements());
        possibleTerminalElements.removeAll(notInOntologies);
        rootFinder.findTerminalElements(possibleTerminalElements);
    }

    private void registerNodeChanged(OWLClass node) {
        nodesToUpdate.add(node);
    }

    private void notifyNodeChanges() {
        nodesToUpdate.forEach(this::fireNodeChanged);
        nodesToUpdate.clear();
    }

    private static boolean isAxiomChange(OWLOntologyChangeRecord rec) {
        return rec.getData() instanceof AxiomChangeData;
    }

    public boolean containsReference(OWLClass object) {
        return entitiesInProjectSignatureByIriIndex
                .getEntityInSignature(object.getIRI())
                .anyMatch(entity -> entity.equals(object));
    }

    public Set<OWLClass> getRoots() {
        return Collections.singleton(root);
    }

    public Set<OWLClass> getChildren(OWLClass object) {
        Set<OWLClass> result;
        if(object.equals(root)) {
            result = new HashSet<>();
            result.addAll(rootFinder.getTerminalElements());
            result.addAll(extractChildren(object));
            result.remove(object);
        }
        else {
            result = extractChildren(object);
            //            result.removeIf(curChild -> getAncestors(object).contains(curChild));
        }

        return result;
    }

    private Set<OWLClass> extractChildren(OWLClass parent) {
        ChildClassExtractor childClassExtractor = new ChildClassExtractor();
        childClassExtractor.setCurrentParentClass(parent);
        projectOntologiesIndex
                .getOntologyIds()
                .flatMap(ontId -> axiomsByEntityReferenceIndex.getReferencingAxioms(parent, ontId))
                .filter(OWLAxiom::isLogicalAxiom)
                .forEach(ax -> ax.accept(childClassExtractor));
        return childClassExtractor.getResult();
    }

    public Set<OWLClass> getEquivalents(OWLClass object) {
        Set<OWLClass> result = new HashSet<>();
        projectOntologiesIndex
                .getOntologyIds()
                .flatMap(ontId -> equivalentClassesAxiomsIndex.getEquivalentClassesAxioms(object, ontId))
                .flatMap(cls -> cls.getClassExpressions()
                                   .stream())
                .filter(cls -> !cls.equals(object))
                .filter(OWLClassExpression::isNamed)
                .map(OWLClassExpression::asOWLClass)
                .forEach(result::add);

        Set<OWLClass> ancestors = getAncestors(object);
        if(ancestors.contains(object)) {
            result.addAll(ancestors.stream()
                                   .filter(cls -> getAncestors(cls).contains(object))
                                   .collect(toList()));
            result.remove(object);
            result.remove(root);
        }
        return result;
    }

}

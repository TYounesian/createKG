package nl.vu.kai;

import de.tu_dresden.inf.lat.evee.proofGenerators.ELKProofGenerator;
import de.tu_dresden.inf.lat.evee.proofs.data.exceptions.ProofGenerationFailedException;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.stream.Collectors;

public class UnionOfJustifications {
    public static void main(String[] args) throws OWLOntologyCreationException, ProofGenerationFailedException, FileNotFoundException, OWLOntologyStorageException {
        System.out.println("Currently only supports ELK as reasoner");
        System.out.println();
        if (args.length == 0) {
            System.out.println("Usage: ");
            System.out.println(UnionOfJustifications.class.getCanonicalName() + " OWL_FILE CLASS INDIVIDUAL OUTPUT_FILE");
            System.exit(0);
        }

        System.out.println("Loading ontology...");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(args[0]));

        ELKProofGenerator proofGenerator = new ELKProofGenerator();
        proofGenerator.setOntology(ontology);
        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLAxiom entailment = factory.getOWLClassAssertionAxiom(
                factory.getOWLClass(IRI.create(args[1])),
                factory.getOWLNamedIndividual(IRI.create(args[2]))
        );

        System.out.println("Computing justifications...");

        IProof<OWLAxiom> derivationStructure = proofGenerator.getProof(entailment);

        Set<OWLAxiom> result = derivationStructure.getInferences()
                .stream()
                .filter(x -> x.getPremises().isEmpty())
                .map(IInference::getConclusion)
                .filter(x -> x.isOfType(AxiomType.ABoxAxiomTypes))
                .filter(ontology::containsAxiom)
                .collect(Collectors.toSet());

        OWLOntology just = manager.createOntology();
        just.addAxioms(result);

        System.out.println("Saving...");

        manager.saveOntology(just, new TurtleDocumentFormat(), new FileOutputStream(new File(args[3])));
    }
}

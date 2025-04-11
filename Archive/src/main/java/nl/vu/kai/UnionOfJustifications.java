package nl.vu.kai;

import de.tu_dresden.inf.lat.evee.proofGenerators.ELKProofGenerator;
import de.tu_dresden.inf.lat.evee.proofs.data.exceptions.ProofGenerationFailedException;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IInference;
import de.tu_dresden.inf.lat.evee.proofs.interfaces.IProof;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

public class UnionOfJustifications {
    public static void main(String[] args) throws OWLOntologyCreationException, ProofGenerationFailedException, FileNotFoundException, OWLOntologyStorageException {
        System.out.println("Currently only supports ELK as reasoner");
        System.out.println();
        if (args.length == 0) {
            System.out.println("Usage: ");
            System.out.println(UnionOfJustifications.class.getCanonicalName() + " OWL_FILE CLASS INDIVIDUALS_FILE OUTPUT_FILE");
            System.exit(0);
        }

        System.out.println("Loading ontology...");
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(args[0]));

        OWLDataFactory factory = manager.getOWLDataFactory();

        ELKProofGenerator proofGenerator = new ELKProofGenerator();
        proofGenerator.setOntology(ontology);

        OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(ontology);

        OWLClass clazz = factory.getOWLClass(IRI.create(args[1]));

        PrintWriter writer = new PrintWriter(new FileOutputStream(args[2]));

        for(OWLIndividual individual:reasoner.instances(clazz).collect(Collectors.toSet())) {

            writer.println(individual+":");

            OWLOntology justification =
                    forIndividual(factory,ontology,clazz, individual,proofGenerator,manager);

            for(OWLAxiom axiom: justification.getAxioms()){
                if(axiom instanceof OWLObjectPropertyAssertionAxiom){
                    OWLObjectPropertyAssertionAxiom prp = (OWLObjectPropertyAssertionAxiom) axiom;
                    writer.println(prp.getSubject()+" "+prp.getProperty()+" "+prp.getObject());
                } else if (axiom instanceof OWLClassAssertionAxiom) {
                    OWLClassAssertionAxiom ax = (OWLClassAssertionAxiom) axiom;
                    writer.println(ax.getIndividual()+" rdf:type "+ax.getClassExpression());
                }
            }
            writer.println();

        }
        writer.close();
    }

    private static OWLOntology forIndividual(
            OWLDataFactory factory,
            OWLOntology ontology,
            OWLClass clazz,
            OWLIndividual individual,
            ELKProofGenerator proofGenerator,
            OWLOntologyManager manager) throws ProofGenerationFailedException, OWLOntologyCreationException {

        OWLAxiom entailment = factory.getOWLClassAssertionAxiom(
                factory.getOWLClass(clazz),
                individual
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

        return just;
    }
}

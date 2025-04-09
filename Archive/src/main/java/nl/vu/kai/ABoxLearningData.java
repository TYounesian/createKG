package nl.vu.kai;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Load ontology
 * Extract ABox part and write to one file
 * Write classification result in other file
 */
public class ABoxLearningData {
    public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
        if(args.length==0){
            System.out.println("Usage: ");
            System.out.println(ABoxLearningData.class.getCanonicalName()+" ONTOLOGY_NAME ELK|HERMIT");
            System.exit(0);
        }

        File file = new File(args[0]);

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology ont = man.loadOntologyFromOntologyDocument(file);
        OWLDataFactory factory = man.getOWLDataFactory();

        OWLOntology abox = man.createOntology();
        ont.getABoxAxioms(Imports.EXCLUDED).forEach(x -> abox.addAxiom(x));

        man.saveOntology(abox, new TurtleDocumentFormat(), new FileOutputStream(new File(file.getName()+".abox.ttl")));

        OWLReasoner reasoner=null;

        // now the reasoning part
        switch(args[1]) {
            case "ELK":
                reasoner = new ElkReasonerFactory().createReasoner(ont);
                break;
            case "HERMIT":
                reasoner = new ReasonerFactory().createReasoner(ont);
                break;
            default: System.out.println("Unexpected reasoner string: "+args[1]); System.exit(1);
        }

        reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);

        //Map<OWLClass, Set<OWLNamedIndividual>> assertions = new HashMap<>();

        int indNumber = ont.getIndividualsInSignature().size();


        for(OWLClass clazz : ont.getClassesInSignature(Imports.INCLUDED)) {
            Set<OWLNamedIndividual> instances = reasoner.getInstances(clazz).getFlattened();
            if(instances.size()>0 && instances.size()<indNumber && !allAsserted(ont,clazz,instances,factory)) {

                File clazzFile = new File(file.getName()+"_"+clazz.getIRI().getShortForm()+".txt");
                FileOutputStream output = new FileOutputStream(clazzFile);
                PrintWriter writer = new PrintWriter(output);
                writer.println(clazz.getIRI());
                writer.println();
                instances.forEach(i -> writer.println(i.getIRI()));
                writer.close();
                output.close();

            } else
                System.out.println("Skipped since not interesting: "+clazz);

        }
    }

    private static boolean allAsserted(OWLOntology ont, OWLClass clazz, Set<OWLNamedIndividual> instances, OWLDataFactory factory) {
        for(OWLNamedIndividual ind:instances) {
            if(!ont.containsAxiom(factory.getOWLClassAssertionAxiom(clazz,ind)))
                return false;
        }
        return true;
    }


}
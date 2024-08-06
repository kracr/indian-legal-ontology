/* The main class which defines the ontology object and allows for creation and manipulation of the ontology using OWL API.
 * There are methods here which handle addition of individuals, taxonomic and non-taxonomic relations, domain and range axioms. Specific data property axioms and annotation axioms
 * are also supported. There are methods supporting bulk addition of subclasses from a text file and also other methods which support bulk addition of subclasses with definitions (from two
 * separate files.) Certain methods for facilitating search and traversal of the ontology graph are also included here. The examples of usage can be seen from the other Java files which
 * take up the base SALI ontology and make the specified additions using this library. Lastly, the URIs for each new addition are uniquely generated and are returned by the calling methods
 * for further reference in the calling program.
 */

package onted;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MyOntology {
	private OWLOntologyManager manager;
	private OWLOntology ontology;
	private OWLDataFactory factory;
	private String basePrefix;
	
	public MyOntology(String owlFile, String IRIPrefix) throws OWLOntologyCreationException {
		// Create an OWLOntologyManager instance
		manager = OWLManager.createOWLOntologyManager();
		// Load your existing ontology
		File inputOntologyFile = new File(owlFile);
		ontology = manager.loadOntologyFromOntologyDocument(inputOntologyFile);
        factory = manager.getOWLDataFactory();
        basePrefix = IRIPrefix;
	}

	public MyOntology(String IRIPrefix) throws OWLOntologyCreationException {
		// Create an OWLOntologyManager instance
		manager = OWLManager.createOWLOntologyManager();
		// Create new ontology
		basePrefix = IRIPrefix;
		ontology = manager.createOntology(generateUniqueIRI(basePrefix));
        factory = manager.getOWLDataFactory();
	}

	public void saveOntology(String myFile) throws OWLOntologyStorageException, FileNotFoundException {
        // Save the combined ontology
        File outputOntologyFile = new File(myFile);
        manager.saveOntology(ontology, new FileOutputStream(outputOntologyFile));
	}

	public IRI generateUniqueIRI(String basePrefix) {
	    IRI uniqueIRI;
	    do {
	        uniqueIRI = IRI.create(basePrefix + UUID.randomUUID().toString());
	    } while (ontology.containsEntityInSignature(uniqueIRI));
	    return uniqueIRI;
	}
	
    public void importOntology(String importOntologyIRIString) throws OWLOntologyCreationException {
    	IRI importOntologyIRI = IRI.create(importOntologyIRIString);
        // Create an OWLImportsDeclaration for the ontology to be imported
        OWLImportsDeclaration importDeclaration = factory.getOWLImportsDeclaration(importOntologyIRI);

        // Add the import declaration to the main ontology
        manager.applyChange(new AddImport(ontology, importDeclaration));
    }

	public ArrayList<String> entitiesFromFile(String myFile, String myPrefix, String mySuffix) throws IOException {
		// Read the classes from the text file and add them to the ontology
		File entitiesTextFile = new File(myFile);
		ArrayList<String> entities = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(entitiesTextFile));
		String line;
		while ((line = br.readLine()) != null) {
			// Trim leading and trailing whitespaces
			line = myPrefix + line.trim() + mySuffix;
			if (!line.isEmpty())
				entities.add(line);
		}
		br.close();
		return entities;
	}

	public OWLDatatype getDatatype(String data) {
		OWLDatatype myType = null;
		if (data.equalsIgnoreCase("date"))
			myType = manager.getOWLDataFactory().getOWLDatatype(XSDVocabulary.DATE.getIRI());
		else
			if (data.equalsIgnoreCase("string"))
				myType = manager.getOWLDataFactory().getOWLDatatype(XSDVocabulary.STRING);
		return myType;
	}
	
	public OWLLiteral getOWLLiteral(String myLiteral) {
        // Create an OWLLiteral from a string
        OWLLiteral literal = factory.getOWLLiteral(myLiteral);
        return literal;
	}

    public ArrayList<String> getSubclasses(String myParentIRI) {

        // Check if the parent IRI exists in the ontology
		IRI parentIRI = IRI.create(myParentIRI);
		OWLClass parentClass = factory.getOWLClass(parentIRI);
        if (!ontology.containsClassInSignature(parentIRI)) {
        	throw new RuntimeException("Error: Parent IRI not found in the ontology.");
        }
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

        Set<String> subclassesIRIs = new HashSet<>();

        // Use the reasoner to get the subclasses
        NodeSet<OWLClass> subclassesNodeSet = reasoner.getSubClasses(parentClass, false);
        for (Node<OWLClass> subclassNode : subclassesNodeSet) {
            // Filter out OWL:Nothing and add IRIs to the set
            subclassNode.entities().forEach(owlClass -> {
                if (!owlClass.isOWLNothing()) {
                    subclassesIRIs.add(owlClass.getIRI().toString());
                }
            });
        }
        return new ArrayList<String>(subclassesIRIs);
    }

    public List<String> getSuperclasses(String myClassIRI) {
        // Check if the parent IRI exists in the ontology
		IRI classIRI = IRI.create(myClassIRI);
		OWLClass owlClass = factory.getOWLClass(classIRI);
        if (!ontology.containsClassInSignature(classIRI)) {
        	throw new RuntimeException("Error: Parent IRI not found in the ontology.");
        }
    	OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);

        // Fetch the direct superclasses of the given class
        NodeSet<OWLClass> superClassesNodeSet = reasoner.getSuperClasses(owlClass, true);
        return superClassesNodeSet.entities()
                                  .filter(owlSuperClass -> !owlSuperClass.isOWLNothing())
                                  .map(owlSuperClass -> owlSuperClass.getIRI().toString())
                                  .collect(Collectors.toList());
    }

    public List<String> getClassesByLabel(String label) {
        return ontology.classesInSignature()
                       .filter(owlClass -> hasLabel(owlClass, label))
                       .map(owlClass -> owlClass.getIRI().toString())
                       .collect(Collectors.toList());
    }

    private boolean hasLabel(OWLClass owlClass, String label) {
        return ontology.annotationAssertionAxioms(owlClass.getIRI())
                       .filter(axiom -> axiom.getProperty().getIRI().equals(OWLRDFVocabulary.RDFS_LABEL.getIRI()))
                       .filter(axiom -> axiom.getValue() instanceof OWLLiteral)
                       .map(axiom -> (OWLLiteral) axiom.getValue())
                       .anyMatch(literal -> literal.getLiteral().toLowerCase().contains(label.toLowerCase()));
    }

	public void addNewAxiom(OWLAxiom myAxiom) {
		manager.addAxiom(ontology, myAxiom);
	}

	public ArrayList<String> addSubClass(String myParentIRI, ArrayList<String> myClasses) {
		ArrayList<String> IRI_list = new ArrayList<String>();
		for (String className : myClasses) {
			IRI_list.add(addSubClass(myParentIRI, className));
		}
		return IRI_list;
	}

	public String addSubClass(String myParentIRI, String className) {
		// Check if the parent IRI exists in the ontology
		IRI parentIRI = IRI.create(myParentIRI);
		OWLClass parentClass = factory.getOWLClass(parentIRI);
        if (!ontology.containsClassInSignature(parentIRI)) {
        	throw new RuntimeException("Error: Parent IRI not found in the ontology.");
        }

		// Create a new IRI using UUID
		IRI classIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());

		// Create the class
		OWLClass newClass = factory.getOWLClass(classIRI);

		// Create a subclass axiom
		OWLSubClassOfAxiom subclassAxiom = factory.getOWLSubClassOfAxiom(newClass, parentClass);
		addNewAxiom(subclassAxiom);

		// Assign the rdfs:label for the new class
		OWLAnnotation labelAnnotation = factory.getOWLAnnotation(
				factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
				factory.getOWLLiteral(className)
				);
		OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(classIRI, labelAnnotation);
		addNewAxiom(annotationAxiom);
		return classIRI.toString();
	}

	public String addIndividual(String myTypeIRI, String myIndividual) {
        // Check if the parent IRI exists in the ontology
		ArrayList<String> IRI_list = new ArrayList<String>();
        IRI typeIRI = IRI.create(myTypeIRI);
        OWLClass parentClass = factory.getOWLClass(typeIRI);
        if (!ontology.containsClassInSignature(typeIRI)) {
        	throw new RuntimeException("Error: Type not found in the ontology.");
        }
        // Construct the IRI using BASE_PREFIX and a UUID
        IRI individualIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(individualIRI);
        
        // Add individual as instance of the class
        OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(parentClass, individual);
        manager.addAxiom(ontology, classAssertion);

        // Add RDFS label annotation to the individual with the entity name
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(myIndividual));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(individualIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);
        return individualIRI.toString();
	}

	public String addIndividual(String myIndividual) {
        // Construct the IRI using BASE_PREFIX and a UUID
        IRI individualIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(individualIRI);

        // Add RDFS label annotation to the individual with the entity name
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(myIndividual));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(individualIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);
        return individualIRI.toString();
	}

	public ArrayList<String> addIndividuals(String myTypeIRI, ArrayList<String> myIndividuals) {
		ArrayList<String> IRI_list = new ArrayList<String>();
        for (String entity : myIndividuals) {
        	IRI_list.add(addIndividual(myTypeIRI, entity));
        }
        return IRI_list;
	}

	public ArrayList<String> addIndividuals(ArrayList<String> myIndividuals) {
		ArrayList<String> IRI_list = new ArrayList<String>();
        for (String entity : myIndividuals) {
        	IRI_list.add(addIndividual(entity));
        }
        return IRI_list;
	}
	
	public void addIndividualByIRI(String individualIRIString, String label) {
        // Construct the IRI using BASE_PREFIX and a UUID
		IRI individualIRI = IRI.create(individualIRIString);
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(individualIRI);

        // Add RDFS label annotation to the individual with the entity name
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(individualIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);
	}
	
	public void setType(String entityIRIString, String myTypeIRI) {
        IRI typeIRI = IRI.create(myTypeIRI);
        OWLClass parentClass = factory.getOWLClass(typeIRI);
//        if (!ontology.containsClassInSignature(typeIRI)) {
//        	throw new RuntimeException("Error: Type not found in the ontology.");
//        }
        
        IRI entityIRI = IRI.create(entityIRIString);

        // Check if the IRI belongs to an individual
        if (ontology.containsIndividualInSignature(entityIRI)) {
            OWLNamedIndividual individual = factory.getOWLNamedIndividual(entityIRI);
            OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(parentClass, individual);
            manager.addAxiom(ontology, classAssertion);
        }
        // Check if the IRI belongs to a class
        else if (ontology.containsClassInSignature(entityIRI)) {
            OWLClass owlClass = factory.getOWLClass(entityIRI);
            OWLSubClassOfAxiom subClassAxiom = factory.getOWLSubClassOfAxiom(owlClass, parentClass);
            manager.addAxiom(ontology, subClassAxiom);
        }
	}

    public void assertSomeValuesFromAxiom(String subjectIRIString, String objectIRIString, String propertyIRIString) {
        // Asserts that every instance of ClassA is related via specified object property to at least one instance of ClassB
        OWLClass classA = factory.getOWLClass(IRI.create(subjectIRIString));
        OWLClass classB = factory.getOWLClass(IRI.create(objectIRIString));
        OWLObjectProperty property = factory.getOWLObjectProperty(IRI.create(propertyIRIString));

        // Creating a SomeValuesFrom restriction: classA SubClassOf property some classB
        OWLObjectSomeValuesFrom restriction = factory.getOWLObjectSomeValuesFrom(property, classB);
        OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(classA, restriction);
        addNewAxiom(axiom);
    }

    public void assertHasValueAxiom(String classIRIString, String individualIRIString, String propertyIRIString) {
        OWLClass owlClass = factory.getOWLClass(IRI.create(classIRIString));
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(IRI.create(individualIRIString));
        OWLObjectProperty property = factory.getOWLObjectProperty(IRI.create(propertyIRIString));

        // Creating a HasValue restriction: class SubClassOf property value individual
        OWLObjectHasValue hasValueRestriction = factory.getOWLObjectHasValue(property, individual);
        OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(owlClass, hasValueRestriction);
        addNewAxiom(axiom);
    }

    
    public void assertDataPropertyAxiom(String subjectIRIString, OWLLiteral myLiteral, String propertyIRIString) {
    	// Asserts the axiom stating every instance of the given class is related via specified data property to the provided literal 
        OWLClass owlClass = factory.getOWLClass(IRI.create(subjectIRIString));
        OWLDataProperty property = factory.getOWLDataProperty(IRI.create(propertyIRIString));

        // Creating a HasValue restriction: owlClass SubClassOf property value literal
        OWLDataHasValue restriction = factory.getOWLDataHasValue(property, myLiteral);
        OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(owlClass, restriction);
        addNewAxiom(axiom);
    }

    public void assertObjectPropertyAxiom(String subjectIRIString, String objectIRIString, String propertyIRIString) {
        OWLNamedIndividual individual1 = factory.getOWLNamedIndividual(subjectIRIString);
        OWLNamedIndividual individual2 = factory.getOWLNamedIndividual(objectIRIString);
        OWLObjectProperty property = factory.getOWLObjectProperty(propertyIRIString);

        OWLObjectPropertyAssertionAxiom axiom = factory.getOWLObjectPropertyAssertionAxiom(property, individual1, individual2);
        addNewAxiom(axiom);
    }

	public String addNewObjectPropertyWithDR(String propertyName, String domainClassIRIString, String rangeClassIRIString) {
        OWLClass domainClass = factory.getOWLClass(IRI.create(domainClassIRIString));
        OWLClass rangeClass = factory.getOWLClass(IRI.create(rangeClassIRIString));

        if (!ontology.containsClassInSignature(domainClass.getIRI()) || !ontology.containsClassInSignature(rangeClass.getIRI())) {
            throw new RuntimeException("Either domain or range class IRI does not exist in the ontology.");
        }

        // Construct the IRI for the property using BASE_PREFIX and a UUID
        IRI propertyIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLObjectProperty property = factory.getOWLObjectProperty(propertyIRI);

        // Set domain and range for the property
        OWLObjectPropertyDomainAxiom domainAxiom = factory.getOWLObjectPropertyDomainAxiom(property, domainClass);
        OWLObjectPropertyRangeAxiom rangeAxiom = factory.getOWLObjectPropertyRangeAxiom(property, rangeClass);
        addNewAxiom(domainAxiom);
        addNewAxiom(rangeAxiom);


        // Add RDFS label annotation to the property
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(propertyName));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(propertyIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);

        assertSomeValuesFromAxiom(domainClassIRIString, rangeClassIRIString, propertyIRI.toString());
        
        return propertyIRI.toString();
	}

	public String addNewObjectPropertyWithDR(String propertyName, ArrayList<String> domainClassIRIStrings, String rangeClassIRIString) {
		Set<OWLClassExpression> domainSet = new HashSet<>();
		OWLClass domainClass = null;
		Iterator it = domainClassIRIStrings.iterator();
		while (it.hasNext()) {
			domainClass = factory.getOWLClass(IRI.create((String) it.next()));
			domainSet.add(domainClass);
		}
		OWLClassExpression unionOfDomains = factory.getOWLObjectUnionOf(domainSet);
        
        OWLClass rangeClass = factory.getOWLClass(IRI.create(rangeClassIRIString));

//        if (!ontology.containsClassInSignature(domainClass.getIRI()) || !ontology.containsClassInSignature(rangeClass.getIRI())) {
//            throw new RuntimeException("Either domain or range class IRI does not exist in the ontology.");
//        }

        // Construct the IRI for the property using BASE_PREFIX and a UUID
        IRI propertyIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLObjectProperty property = factory.getOWLObjectProperty(propertyIRI);

        // Set domain and range for the property
        OWLObjectPropertyDomainAxiom domainAxiom = factory.getOWLObjectPropertyDomainAxiom(property, unionOfDomains);
        OWLObjectPropertyRangeAxiom rangeAxiom = factory.getOWLObjectPropertyRangeAxiom(property, rangeClass);
        addNewAxiom(domainAxiom);
        addNewAxiom(rangeAxiom);


        // Add RDFS label annotation to the property
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(propertyName));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(propertyIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);
        it = domainClassIRIStrings.iterator();
		while (it.hasNext()) {
			assertSomeValuesFromAxiom((String) it.next(), rangeClassIRIString, propertyIRI.toString());
		}
        
        return propertyIRI.toString();
	}

	public String addNewObjectProperty(String propertyName, String subjectIRIString, String objectIRIString) {
        IRI iri1 = IRI.create(subjectIRIString);
        IRI iri2 = IRI.create(objectIRIString);
        
        // Create IRI for the object property using base prefix and UUID
        IRI propertyIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLObjectProperty property = factory.getOWLObjectProperty(propertyIRI);

        // Add RDFS label annotation to the property
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(propertyName));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(propertyIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);

        // Check if they are individuals or classes
        boolean isIndividual1 = ontology.containsIndividualInSignature(iri1);
        boolean isIndividual2 = ontology.containsIndividualInSignature(iri2);
        
        // Both are individuals
        if (isIndividual1 && isIndividual2) {
            OWLNamedIndividual individual1 = factory.getOWLNamedIndividual(iri1);
            OWLNamedIndividual individual2 = factory.getOWLNamedIndividual(iri2);
            OWLObjectPropertyAssertionAxiom assertion = factory.getOWLObjectPropertyAssertionAxiom(property, individual1, individual2);
            addNewAxiom(assertion);
        } 
        // Both are classes
        else if (!isIndividual1 && !isIndividual2) {
        	addNewObjectPropertyWithDR(propertyName, subjectIRIString, objectIRIString);
        } 
        // Mix of individual and class (This case might not make semantic sense in many ontologies -- check how to handle this!)
        else {
        	System.out.println("Subject: " + isIndividual1 + " Object: " + isIndividual2);
        	throw new RuntimeException("Warning: Attempting to connect an individual and a class, which might not make semantic sense.");
        }
        return propertyIRI.toString();
	}

	public String addNewDataProperty(String propertyName, String subjectIRIString, OWLLiteral myLiteral) {
		IRI subjectIRI = IRI.create(subjectIRIString);

        // Create IRI for the object property using base prefix and UUID
        IRI propertyIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLDataProperty property = factory.getOWLDataProperty(propertyIRI);
        
        // Add RDFS label annotation to the property
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(propertyName));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(propertyIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);

		// Check if subject is an individual or class
        boolean isIndividual = ontology.containsIndividualInSignature(subjectIRI);
        if (isIndividual) {
        	OWLNamedIndividual individual = factory.getOWLNamedIndividual(subjectIRI);

            // Create the assertion (relation) between the named individual and the attribute value
            OWLDataPropertyAssertionAxiom assertion = factory.getOWLDataPropertyAssertionAxiom(property, individual, myLiteral);
            addNewAxiom(assertion);
        }
        else {
            //OWLClass subjectClass = factory.getOWLClass(subjectIRI);
            assertDataPropertyAxiom(subjectIRIString, myLiteral, propertyIRI.toString());
        }
        return propertyIRI.toString();
	}

	public String addNewDataPropertyWithDR(String propertyName, String subjectIRIString, OWLDatatype rangeType) {
        // Create IRI for the data property using base prefix and UUID
        IRI propertyIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLDataProperty dataProperty = factory.getOWLDataProperty(propertyIRI);
        
        // Add RDFS label annotation to the property
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(propertyName));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(propertyIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);
        // Set the domain (class) of the property
        OWLClass domainClass = factory.getOWLClass(IRI.create(subjectIRIString));
        OWLDataPropertyDomainAxiom domainAxiom = factory.getOWLDataPropertyDomainAxiom(dataProperty, domainClass);
        //Newer syntax
        //manager.addAxiom(ontology, domainAxiom);
        addNewAxiom(domainAxiom);

        // Set the range (data type) of the property
        OWLDataPropertyRangeAxiom rangeAxiom = factory.getOWLDataPropertyRangeAxiom(dataProperty, rangeType);
        //manager.addAxiom(ontology, rangeAxiom);
        addNewAxiom(rangeAxiom);
        return dataProperty.toString();
	}

	public String addNewDataPropertyWithDR(String propertyName, ArrayList<String> subjectIRIStrings, OWLDatatype rangeType) {
		Set<OWLClassExpression> domainSet = new HashSet<>();
		OWLClass domainClass = null;
		Iterator it = subjectIRIStrings.iterator();
		while (it.hasNext()) {
			domainClass = factory.getOWLClass(IRI.create((String) it.next()));
			domainSet.add(domainClass);
		}
		OWLClassExpression unionOfDomains = factory.getOWLObjectUnionOf(domainSet);
        // Create IRI for the data property using base prefix and UUID
        IRI propertyIRI = generateUniqueIRI(basePrefix + UUID.randomUUID().toString());
        OWLDataProperty dataProperty = factory.getOWLDataProperty(propertyIRI);
        
        // Add RDFS label annotation to the property
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(propertyName));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(propertyIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);
        // Set the domain (class) of the property
        //OWLClass domainClass = factory.getOWLClass(IRI.create(subjectIRIString));
        OWLDataPropertyDomainAxiom domainAxiom = factory.getOWLDataPropertyDomainAxiom(dataProperty, unionOfDomains);
        //Newer syntax
        //manager.addAxiom(ontology, domainAxiom);
        addNewAxiom(domainAxiom);

        // Set the range (data type) of the property
        OWLDataPropertyRangeAxiom rangeAxiom = factory.getOWLDataPropertyRangeAxiom(dataProperty, rangeType);
        //manager.addAxiom(ontology, rangeAxiom);
        addNewAxiom(rangeAxiom);
        return dataProperty.toString();
	}

	public void addAnnotationProperty(IRI annotationIRI, String label) {

        // Create the new annotation property
        OWLAnnotationProperty newAnnotationProperty = factory.getOWLAnnotationProperty(annotationIRI);

        // Optionally, add a label to the annotation property
        OWLAnnotationProperty labelProperty = factory.getRDFSLabel();
        OWLLiteral labelLiteral = factory.getOWLLiteral(label);
        OWLAnnotation annotation = factory.getOWLAnnotation(labelProperty, labelLiteral);
        OWLAnnotationAssertionAxiom axiom = factory.getOWLAnnotationAssertionAxiom(annotationIRI, annotation);

        // Add the property and label to the ontology
        addNewAxiom(factory.getOWLDeclarationAxiom(newAnnotationProperty));
		addNewAxiom(axiom);
	}

	public void annotateClass(String entityIRIString, String annotationValue) {
		// Check if the parent IRI exists in the ontology
		IRI entityIRI = IRI.create(entityIRIString);
		OWLClass entityClass = factory.getOWLClass(entityIRI);
        if (!ontology.containsClassInSignature(entityIRI)) {
        	throw new RuntimeException("Error: Parent IRI not found in the ontology.");
        }

		// Create the annotation
		OWLAnnotationProperty labelProperty = factory.getRDFSLabel();
		OWLLiteral labelLiteral = factory.getOWLLiteral(annotationValue);
		OWLAnnotation annotation = factory.getOWLAnnotation(labelProperty, labelLiteral);
		OWLAnnotationAssertionAxiom axiom = factory.getOWLAnnotationAssertionAxiom(entityIRI, annotation);

		// Add the annotation to the ontology
		addNewAxiom(axiom);
	}
	
	public void labelEntity(String entityIRIString, String myLabel, boolean allowAccents) {
		// Check if the parent IRI exists in the ontology
		IRI entityIRI = IRI.create(entityIRIString);

		String label = myLabel.substring(0);
		if (!allowAccents) {
			label = StringUtils.stripAccents(myLabel);
		}
        
        // Add RDFS label annotation to the property
        OWLAnnotation labelAnnotation = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral(label));
        OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(entityIRI, labelAnnotation);
        addNewAxiom(annotationAxiom);
	}

	public void addSKOSDefinitionAnnotation(String entityIRIString, String definition) {
		// Check if the parent IRI exists in the ontology
		IRI entityIRI = IRI.create(entityIRIString);
		OWLClass entityClass = factory.getOWLClass(entityIRI);
        if (!ontology.containsClassInSignature(entityIRI)) {
        	throw new RuntimeException("Error: Parent IRI not found in the ontology.");
        }
        
		// Create the SKOS definition annotation property and literal
		OWLAnnotationProperty skosDefinition = factory.getOWLAnnotationProperty(SKOSVocabulary.DEFINITION.getIRI());
		OWLLiteral definitionLiteral = factory.getOWLLiteral(definition);

		// Create the annotation assertion axiom
		OWLAnnotation annotation = factory.getOWLAnnotation(skosDefinition, definitionLiteral);
		OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(entityIRI, annotation);
		addNewAxiom(annotationAxiom);
	}

	public void addSKOSAltLabelAnnotation(String entityIRIString, String altLabel) {
		// Check if the parent IRI exists in the ontology
		IRI entityIRI = IRI.create(entityIRIString);
		OWLClass entityClass = factory.getOWLClass(entityIRI);
        if (!ontology.containsClassInSignature(entityIRI)) {
        	throw new RuntimeException("Error: Parent IRI not found in the ontology.");
        }
        
		// Create the SKOS definition annotation property and literal
		OWLAnnotationProperty altLabelProp = factory.getOWLAnnotationProperty(SKOSVocabulary.ALTLABEL.getIRI());
		OWLLiteral labelLiteral = factory.getOWLLiteral(altLabel);

		// Create the annotation assertion axiom
		OWLAnnotation annotation = factory.getOWLAnnotation(altLabelProp, labelLiteral);
		OWLAnnotationAssertionAxiom annotationAxiom = factory.getOWLAnnotationAssertionAxiom(entityIRI, annotation);
		addNewAxiom(annotationAxiom);
	}
}
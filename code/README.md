This is a Maven project.
The MyOntology class is the main library which can be used for any ontology building work.
-	create a new ontology or use an existing ontology
-	generate new IRI while avoiding any collisions
-	search subclasses and super-classes of a given class
-	search classes by label
-	bulk addition of new subclasses under a given class
-	bulk addition of individuals, with or without a type
-	set type of a given entity
-	add object property
-	add data property
-	assert domain and range with object and data property
-	add label and annotation property

The GeoNames data has been queried and stored in text files.

Execution:
BatchUpdateFinal_paper.java takes the base ontology and outputs the extended version.

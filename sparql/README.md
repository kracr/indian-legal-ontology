**Try a few SPARQL queries!
**

**List all high courts.
**
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ex: <http://lmss.sali.org/>

SELECT ?label
WHERE {
  ?entity rdf:type ?type .
  ?type rdfs:label "High Court" .
  ?entity rdfs:label ?label .
}

**List the location of a given court.
**
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ex: <http://lmss.sali.org/>
PREFIX geoNames: <http://www.geonames.org/ontology#>

SELECT DISTINCT ?locationLabel
WHERE {
  ?indianCourt rdfs:label "Bombay High Court" .
  ?indianCourt rdf:type ?indianCourtType .
  ?indianCourtType rdfs:label "Indian court" .
  ?indianCourt geoNames:locatedIn ?location .
  ?location rdfs:label ?locationLabel .
}

**List all courts under a given high court.
**
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX ex: <http://lmss.sali.org/>

SELECT ?courtLabel
WHERE {
  # Identify the property 'hasPrecedenceOver' by its label
  ?hasPrecedenceOverProperty rdfs:label "hasPrecedenceOver" .
  
  # Identify the high court
  ?highCourt rdfs:label "Allahabad High Court" .
  ?highCourt rdf:type ?highCourtType .
  ?highCourtType rdfs:label "High Court" .

  # Find courts linked by 'hasPrecedenceOver' property
  ?highCourt ?hasPrecedenceOverProperty ?court .
  
  # Get the label of the linked courts
  ?court rdfs:label ?courtLabel .
}



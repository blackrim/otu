package opentree.otu.constants;

import org.neo4j.graphdb.RelationshipType;

public enum RelType implements RelationshipType {
	CHILDOF,
	METADATAFOR;
}

package opentree.otu.constants;

import org.neo4j.graphdb.RelationshipType;

public enum RelType implements RelationshipType {

	/**
	 * Connects tree nodes to their parents.
	 */
	CHILDOF,
	
	/**
	 * Connects study metadata nodes to the trees that are included in the study.
	 */
	METADATAFOR,
	
	/**
	 * Connects remote study metadata nodes to the local studies that have been imported from them.
	 */
	LOCALCOPYOF;
}

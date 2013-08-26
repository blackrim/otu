package opentree.otu.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import jade.MessageLogger;
import jade.tree.*;

import opentree.otu.DatabaseManager;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.server.plugins.*;

public class studyJsons extends ServerPlugin{
	
	/**
	 * this is a single tree version
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public String putStudyNewickSingle(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j tree id of the tree to be used as the root for the tree.")
			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "The Neo4j node id of the node to be used as the root for the tree.")
			@Parameter(name = "newickString", optional = false) String newickString) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		TreeReader tr = new TreeReader();
		ArrayList<JadeTree> trees = new ArrayList<JadeTree>();
		JadeTree t = tr.readTree(newickString);
		trees.add(t);
		dm.addStudyToDB(trees, studyID);
		return "{\"worked\":1}";
	}
	
	/**
	 * this is single or multiple trees
	 * @param graphDb
	 * @param studyID
	 * @param newickString
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public String putStudyNewickString(@Source GraphDatabaseService graphDb,
			@Description( "The Neo4j tree id of the tree to be used as the root for the tree.")
			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "The Neo4j node id of the node to be used as the root for the tree.")
			@Parameter(name = "newickString", optional = false) String newickString) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		String studytreelist = dm.getStudyTreeList();
		return studytreelist;
	}
	
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public String getStudyTreeList(@Source GraphDatabaseService graphDb) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		String studytreelist = dm.getStudyTreeList();
		return studytreelist;
	}
	
	@Description( "Return a JSON with the studies listed" )
	@PluginTarget( GraphDatabaseService.class )
	public String getStudyList(@Source GraphDatabaseService graphDb) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		String studylist = dm.getStudyList();
		return studylist;
	}
	
	@Description( "Load a nexson file into the graph database" )
	@PluginTarget( GraphDatabaseService.class )
	public String putStudyNexsonFile(@Source GraphDatabaseService graphDb,
			@Description( "study ID")
			@Parameter(name = "studyID", optional = false) String studyID,
			@Description( "Nexson string")
			@Parameter(name = "nexsonString", optional = false) String nexsonString) {
		StringReader sr = new StringReader(nexsonString);
		MessageLogger msgLogger = new MessageLogger("");
		List<JadeTree> trees = null;
		try {
			trees = NexsonReader.readNexson(sr, false, msgLogger);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DatabaseManager dm = new DatabaseManager(graphDb);
		dm.addStudyToDB(trees,studyID);
		return "{\"worked\":1}";
	}
	
	@Description( "Get study metadata" )
	@PluginTarget( GraphDatabaseService.class )
	public String getStudyMetaData(@Source GraphDatabaseService graphDb,
			@Description( "study ID")
			@Parameter(name = "studyID", optional = false) String studyID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		//TODO add that the source don't exist
		String metadata = dm.getStudyMetaData(studyID);
		return metadata;
	}
	
	/**
	 * @param nodeid
	 * @return
	 */
	@Description( "Return a JSON with alternative parents presented" )
	@PluginTarget( GraphDatabaseService.class )
	public String deleteStudyFromStudyID(@Source GraphDatabaseService graphDb,
			@Description( "study ID")
			@Parameter(name = "studyID", optional = false) String studyID) {
		DatabaseManager dm = new DatabaseManager(graphDb);
		dm.deleteStudyFromStudyID(studyID);
		return "{\"worked\":1}";
	}
	
}

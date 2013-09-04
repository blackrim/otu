package opentree.otu.plugins;

import java.io.IOException;
import java.io.StringReader;


import jade.MessageLogger;
import jade.tree.*;
import opentree.otu.DatabaseBrowser;
import opentree.otu.DatabaseManager;
import opentree.otu.GraphDatabaseAgent;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.server.plugins.*;

public class studyJsons extends ServerPlugin {

	@Description("Return JSON containing information about local sources and trees")
	@PluginTarget(GraphDatabaseService.class)
	public String getStudyTreeList(@Source GraphDatabaseService graphDb) {
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		String studytreelist = browser.getJSONOfSourceIdsAndTreeIdsForImportedTrees();
		return studytreelist;
	}

	@Description("Return JSON containing information about local trees")
	@PluginTarget(GraphDatabaseService.class)
	public String getStudyList(@Source GraphDatabaseService graphDb) {
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		String studylist = browser.getJSONOfSourceIdsForImportedTrees();
		return studylist;
	}

	/**
	 * this is a single tree version
	 * 
	 * @param nodeid
	 * @return
	 */
	@Description("Load a single newick tree into the graph")
	@PluginTarget(GraphDatabaseService.class)
	public String putStudyNewickSingle(
			@Source GraphDatabaseService graphDb,
			@Description("A string to be used as the source id for for this source. Source ids must be unique.") @Parameter(name = "sourceId", optional = false) String sourceId,
			@Description("A newick string containing the tree to be added.") @Parameter(name = "newickString", optional = false) String newickString) {

		GraphDatabaseAgent gdb = new GraphDatabaseAgent(graphDb);
		DatabaseManager dm = new DatabaseManager(gdb);

		NexsonSource source = new NexsonSource(sourceId);

		// ArrayList<JadeTree> trees = new ArrayList<JadeTree>();
		// JadeTree t = tr.readTree(newickString);
		// trees.add(t);

		TreeReader tr = new TreeReader();
		source.addTree(tr.readTree(newickString));

		dm.addSource(source, DatabaseManager.LOCAL_LOCATION);
		return "{\"worked\":1}";
	}

	/**
	 * this is single or multiple trees
	 * 
	 * @param graphDb
	 * @param studyID
	 * @param newickString
	 * @return
	 */
	@Description("Incomplete placeholder for multi-tree upload")
	@PluginTarget(GraphDatabaseService.class)
	public String putStudyNewickMultiple(
			@Source GraphDatabaseService graphDb,
			@Description("A string to be used as the source id for for this source. Source ids must be unique.")
			@Parameter(name = "sourceId", optional = false) String sourceId,
			@Description("A newick string containing the tree to be added.")
			@Parameter(name = "newickString", optional = false) String newickString) {

		return null;
	}

	@Description("Load a nexson file into the graph database")
	@PluginTarget(GraphDatabaseService.class)
	public String putStudyNexsonFile(
			@Source GraphDatabaseService graphDb,
			@Description("A string to be used as the source id for for this source. Source ids must be unique.")
			@Parameter(name = "sourceId", optional = false) String sourceId,
			@Description("A nexson string to be parsed")
			@Parameter(name = "nexsonString", optional = false) String nexsonString) {

//		List<JadeTree> trees = null;

		MessageLogger msgLogger = new MessageLogger("");
		StringReader sr = new StringReader(nexsonString);
		NexsonSource source = null;
		try {
			source = NexsonReader.readNexson(sr, sourceId, false, msgLogger);
		} catch (IOException e) {
			return e.toString();
		}

		DatabaseManager dm = new DatabaseManager(graphDb);
		dm.addSource(source, DatabaseManager.LOCAL_LOCATION);

		return "{\"worked\":1}";
	}

	@Description("Get study metadata")
	@PluginTarget(GraphDatabaseService.class)
	public String getStudyMetaData(@Source GraphDatabaseService graphDb,
			@Description("study ID")
			@Parameter(name = "studyID", optional = false) String studyID) {
		
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);

		// TODO add that the source don't exist

		Node sourceMeta = browser.getSourceMetaNode(studyID, DatabaseBrowser.LOCAL_LOCATION);
		String metadata = DatabaseBrowser.getMetadataJSONForSource(sourceMeta);
		return metadata;
	}

	/**
	 * @param nodeid
	 * @return
	 */
	@Description("Return a JSON with alternative parents presented")
	@PluginTarget(GraphDatabaseService.class)
	public String deleteStudyFromStudyID(@Source GraphDatabaseService graphDb,
			@Description("study ID") @Parameter(name = "studyID", optional = false) String studyID) {

		DatabaseManager dm = new DatabaseManager(graphDb);
		DatabaseBrowser browser = new DatabaseBrowser(graphDb);
		Node sourceMeta = browser.getSourceMetaNode(studyID, DatabaseBrowser.LOCAL_LOCATION);

		dm.deleteSource(sourceMeta);

		return "{\"worked\":1}";
	}

}

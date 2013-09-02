package opentree.otu.plugins;

import jade.MessageLogger;
import jade.tree.JadeTree;
import jade.tree.NexsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import opentree.otu.DatabaseIndexer;
import opentree.otu.DatabaseManager;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.rest.repr.ListRepresentation;
import org.neo4j.server.rest.repr.MappingRepresentation;
import org.neo4j.server.rest.repr.Representation;
import org.neo4j.server.rest.repr.RepresentationFormat;
import org.neo4j.server.rest.repr.ValueRepresentation;

import scala.actors.threadpool.Arrays;

/**
 * services for indexing. very preliminary, should probably be reorganized (later).
 * 
 * @author cody
 * 
 */
public class Indexing extends ServerPlugin {

	private String nexsonCommitsURLStr = "https://bitbucket.org/api/2.0/repositories/blackrim/avatol_nexsons/commits";
	private String nexsonsBaseURL = "https://bitbucket.org/api/1.0/repositories/blackrim/avatol_nexsons/raw/";
	
	/**
	 * Index all remote nexsons. This is slow. Should do atomic indexing of nexsons using AJAX on the page for better feedback and efficiency. Leaving this in though, as it is a useful reference for
	 * doing URL access and JSON parsing in Java.
	 * 
	 * @param graphDb
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Deprecated
	@Description("Perform indexing of public nexsons repo")
	@PluginTarget(GraphDatabaseService.class)
	public Representation indexAllPublicStudies(@Source GraphDatabaseService graphDb) throws InterruptedException, IOException, ParseException {

		// will be re-used
		JSONParser parser = new JSONParser();

		// get the commits from the public repo
		BufferedReader nexsonCommits = new BufferedReader(new InputStreamReader(new URL(nexsonCommitsURLStr).openStream()));
		JSONObject commitsJSON = (JSONObject) parser.parse(nexsonCommits);

		// get just the most recent commit
		String mostRecentCommitHash = (String) ((JSONObject) ((JSONArray) commitsJSON.get("values")).get(0)).get("hash");

		// open reader for the nexsons dir
		String nexsonsDirURL = "https://bitbucket.org/api/1.0/repositories/blackrim/avatol_nexsons/raw/" + mostRecentCommitHash + "/";
		BufferedReader nexsonsDir = new BufferedReader(new InputStreamReader(new URL(nexsonsDirURL).openStream()));

		// prepare for indexing all studies
		DatabaseIndexer di = new DatabaseIndexer(graphDb);
		LinkedList<String> indexedStudies = new LinkedList<String>(); // just for testing really
		LinkedList<String> errorStudies = new LinkedList<String>(); // currently not used

		// for each nexson in the latest commit
		String dirEntry = "";
		while (dirEntry != null) {
			dirEntry = nexsonsDir.readLine();
			try {
				Integer.valueOf(dirEntry);
			} catch (NumberFormatException ex) {
				errorStudies.add(dirEntry);
				continue;
			}

			List<JadeTree> trees = readRemoteNexson(nexsonsDirURL + dirEntry);
			String studyId = dirEntry;

			di.addStudyToIndexes(trees, studyId);
			indexedStudies.add(studyId); // remember studies we indexed
			break;
		}

		return ListRepresentation.string(indexedStudies);
	}
	
	/**
	 * Return the url of the most recent commit in the public repo. Facilitates working with these independently in javascript.
	 * @param graphDb
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Description("Return the url of the most recent commit in the public repo")
	@PluginTarget(GraphDatabaseService.class)
	public Representation getMostCurrentNexsonsURL(@Source GraphDatabaseService graphDb) throws InterruptedException, IOException, ParseException {

		JSONParser parser = new JSONParser();

		// get the commits from the public repo
		BufferedReader nexsonCommits = new BufferedReader(new InputStreamReader(new URL(nexsonCommitsURLStr).openStream()));
		JSONObject commitsJSON = (JSONObject) parser.parse(nexsonCommits);

		// get just the most recent commit
		String mostRecentCommitHash = (String) ((JSONObject) ((JSONArray) commitsJSON.get("values")).get(0)).get("hash");

		// open reader for the nexsons dir
		return ValueRepresentation.string(nexsonsBaseURL + mostRecentCommitHash + "/");

	}
	
	/**
	 * Get a list of the nexson files in the public repo commit at the specified url
	 * @param graphDb
	 * @param url
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ParseException
	 */
	@Description("Get a list of the nexsons currently in the public nexsons repo")
	@PluginTarget(GraphDatabaseService.class)
	public Representation getNexsonsListFromURL(@Source GraphDatabaseService graphDb,
			@Description("remote nexson url") @Parameter(name = "url", optional = false) String url) throws IOException {
		
		BufferedReader nexsonsDir = new BufferedReader(new InputStreamReader(new URL(url).openStream()));

		// prepare for indexing all studies
		LinkedList<String> availableStudies = new LinkedList<String>();
		LinkedList<String> errorStudies = new LinkedList<String>(); // currently not used

		// for each nexson in the latest commit
		String dirEntry = "";
		while (dirEntry != null) {
			dirEntry = nexsonsDir.readLine();
			try {
				Integer.valueOf(dirEntry);
				availableStudies.add(dirEntry);
			} catch (NumberFormatException ex) {
				errorStudies.add(dirEntry);
			}
		}

		return ListRepresentation.string(availableStudies);
	}

	/**
	 * Just index a single remote nexson into the local db.
	 * 
	 * @param graphDb
	 * @param url
	 * @param sourceID
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	@Description("Index a single remote nexson into the local db under the specified source id."
			+ "Studies will only be indexed if they have at least one tree. Returns true if the"
			+ "study is indexed, or false if it has no trees. Trees that cannot be read from nexson"
			+ "files that otherwise contain some good trees will be skipped.")
	@PluginTarget(GraphDatabaseService.class)
	public Representation indexSingleNexson(@Source GraphDatabaseService graphDb,
			@Description("remote nexson url") @Parameter(name = "url", optional = false) String url,
			@Description("source id under which this source will be indexed locally") @Parameter(name = "studyID", optional = false) String studyID)
					throws MalformedURLException, IOException {

		DatabaseIndexer di = new DatabaseIndexer(graphDb);
		List<JadeTree> trees = readRemoteNexson(url);
		
		if (trees == null || trees.size() < 1) {
			return ValueRepresentation.bool(false);
		} else {
			di.addStudyToIndexes(trees, studyID);
			return ValueRepresentation.bool(true);
		}
	}

	/**
	 * helper function for reading a nexson from a url
	 * 
	 * @param url
	 * @return Jade trees from nexson
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private List<JadeTree> readRemoteNexson(String url) throws MalformedURLException, IOException {
		BufferedReader nexson = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
		MessageLogger msgLogger = new MessageLogger("");
		
		// TODO: sometimes this returns a null for the first tree, but no errors. Why? Why don't we get an error?
		return NexsonReader.readNexson(nexson, false, msgLogger);
	}
}

package opentree.otu.plugins;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;

public class Indexing extends ServerPlugin {

	// services for indexing the opentree repo. very preliminary, should be reorganized later
	
	@Description( "Perform indexing of public nexsons repo" )
	@PluginTarget( GraphDatabaseService.class )
	public boolean indexRemoteStudies(@Source GraphDatabaseService graphDb) throws InterruptedException {
		Thread.sleep(5000);
		return true;
	}
}

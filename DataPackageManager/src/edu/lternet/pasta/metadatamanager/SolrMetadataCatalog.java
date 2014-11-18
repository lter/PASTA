package edu.lternet.pasta.metadatamanager;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

import edu.lternet.pasta.common.EmlPackageId;
import edu.lternet.pasta.datapackagemanager.solr.index.SolrIndex;
import edu.lternet.pasta.datapackagemanager.solr.search.SimpleSolrSearch;


public class SolrMetadataCatalog implements MetadataCatalog {
	
	/*
	 * Instance variables
	 */
    private String solrUrl = null;

   
    /*
     * Instance methods
     */
    
    public SolrMetadataCatalog(String solrUrl) {
		this.solrUrl = solrUrl;
	}

    
    public String createEmlDocument(EmlPackageId epid, String emlDocument) {
    	return indexEmlDocument(epid, emlDocument);
    }

    
    public String deleteEmlDocument(EmlPackageId epid) {
    	String result = null;
    	
    	SolrIndex solrIndex = new SolrIndex(solrUrl);
    	
    	try {
    		solrIndex.deleteEmlDocument(epid);
    		solrIndex.commit(); // Always commit after individual document deletes
    	}
    	catch (IOException | SolrServerException e) {
    		e.printStackTrace();
    		result = e.getMessage();
    	}
    	
    	return result;
    }
    
    
    private String indexEmlDocument(EmlPackageId epid, String emlDocument) {
    	String result = null;
    	SolrIndex solrIndex = new SolrIndex(solrUrl);
    	
    	try {
    		result = solrIndex.indexEmlDocument(epid, emlDocument);
    		solrIndex.commit(); // Always commit after individual document uploads
    	}
    	catch (IOException | SolrServerException e) {
    		e.printStackTrace();
    		result = e.getMessage();
    	}
    	
    	return result;
    }

    
    public String query(String queryText) {
    	String result = null;
    	
    	SimpleSolrSearch simpleSolrSearch = new SimpleSolrSearch(solrUrl);
    	
    	try {
    		result = simpleSolrSearch.search(queryText);
    	}
    	catch (SolrServerException e) {
    		e.printStackTrace();
    	}
    	
    	return result;
    }

    
    public String updateEmlDocument(EmlPackageId epid, String emlDocument) {
    	return indexEmlDocument(epid, emlDocument);
    }

}
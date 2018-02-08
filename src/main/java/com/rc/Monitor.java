package com.rc;

import java.io.File;
import java.net.URL;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

/**
 * This handles the web pages. 
 * 
 * We use spark to serve pages. It's simple and easy to configure. It's pretty basic
 * we need 1 websockt to handle messaging to the client and one static dir for the actual page
 * 
 * @author richard
 *
 */
public class Monitor implements AutoCloseable {
	
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;

	final Random random ;
	final Gson gson ;
	final Brain brain ;
	final WebSocketServer wss ;

	public Monitor( Brain brain ) {
		this.gson = new Gson() ;
		this.brain = brain ;
		this.random = new Random() ;
		this.wss = new WebSocketServer() ;
	}
	
	public void start() {
		try {			
			spark.Spark.port( 8111 ) ;
			URL mainPage = getClass().getClassLoader().getResource( "index.html" ) ;
			File path = new File( mainPage.getPath() ) ;
			spark.Spark.staticFiles.externalLocation( path.getParent() ) ;
			spark.Spark.webSocket("/live", wss ) ;			
			spark.Spark.get( "/data", this::getData ) ; //, gson::toJson ) ;
			spark.Spark.awaitInitialization() ;
		} catch( Exception ohohChongo ) {
			logger.error( "Server start failure.", ohohChongo );
		}
	}

	public void sendBrainData( int patternIndex ) {
		wss.send( gson.toJson( brain.getNeuronPotentials( patternIndex ) ) ) ;
	}
	/**
	 * get 1 slice of compressed data, with random shear
	 * 
	 */
	public Object getData(Request req, Response rsp) {
		Object rc = null ;
		try {
			rsp.type( "application/json" );	
			rsp.header("expires", "0" ) ;
			rsp.header("cache-control", "no-cache" ) ;
			
			rc = brain.toJson() ;
		} catch ( Throwable t ) {
			logger.warn( "Error processing getItem request", t ) ;
			rsp.status( 400 ) ;	
		}
		return rc ;
	}




	@Override
	public void close() {
		spark.Spark.stop() ;
	}
}

package com.rc.web;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import com.rc.Brain;
import org.la4j.matrix.sparse.CCSMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.Request;
import spark.Response;

import javax.imageio.ImageIO;

/**
 * This handles the web pages. 
 * 
 * We use spark to serve pages. It's simple and easy to configure. It's pretty basic
 * we need 1 websocket to handle messaging to the client and one static dir for the actual page
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
		this.gson = new GsonBuilder().create() ;
		this.brain = brain ;
		this.random = new Random() ;
		this.wss = new WebSocketServer( brain ) ;
	}
	
	public void start() {
		try {			
			spark.Spark.port( 8111 ) ;
			URL mainPage = getClass().getClassLoader().getResource( "index.html" ) ;
			if( mainPage != null ) {
				File path = new File( mainPage.getPath() ) ;
				spark.Spark.staticFiles.externalLocation( path.getParent() ) ;	
			} else {
				spark.Spark.staticFiles.externalLocation( "src/main/resources" ) ;	
			}
			spark.Spark.webSocket("/live", wss ) ;
            spark.Spark.get( "/data", this::getData, gson::toJson ) ;
			spark.Spark.get( "/synapse-map", this::getSynapses ) ;
			spark.Spark.get( "/synapse-graph", this::getGraph ) ;
			spark.Spark.awaitInitialization() ;
		} catch( Exception ohohChongo ) {
			logger.error( "Server start failure.", ohohChongo );
		}
	}

	public void sendBrainData( double clock ) {
		wss.send( gson.toJson( brain.getNeuronPotentials( clock ) ) ) ;
		brain.setFollowing( wss.getFollowing() ) ;
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


	public Object getSynapses( Request req, Response rsp ) throws IOException {

	    logger.info( "Requesting synapse image" ) ;
		int imageType = BufferedImage.TYPE_BYTE_GRAY ;

        CCSMatrix synapses = brain.getSynapses() ;

        final BufferedImage img = new BufferedImage(synapses.columns(),synapses.rows(),imageType);
		Graphics2D graphics = img.createGraphics();
		graphics.setBackground( Color.BLACK ) ;
		try {
            synapses.eachNonZero( (r, c, v) -> {
                        int p = ( (int) (v * 0x7f ) + 0x80 )  ;
                        img.setRGB(r, c, new Color(0, p, p).getRGB());
                    }
            ) ;
        } catch( Throwable t ) {
            logger.error( "Failed to print", t ) ;
        }
		rsp.type( "image/png" );
		rsp.header("expires", "0" ) ;
		rsp.header("cache-control", "no-cache" ) ;

		ImageIO.write( img, "png", rsp.raw().getOutputStream() );

		return rsp ;
	}



	public Object getGraph( Request req, Response rsp ) throws IOException {

		logger.info( "Requesting graph image" ) ;
		int imageType = BufferedImage.TYPE_BYTE_GRAY ;

		CCSMatrix synapses = brain.getSynapses() ;
		int rows = brain.getRows()  ;
		int columns = brain.getColumns()  ;
        int scale = 15 ;
		final BufferedImage img = new BufferedImage( columns*scale, rows*scale, imageType ) ;
		Graphics2D graphics = img.createGraphics() ;
		graphics.setBackground( Color.BLACK ) ;
		try {
			synapses.eachNonZero( (r, c, v) -> {
				int x0 = r / rows ;
				int y0 = r - ( x0 * rows ) ;
				int x1 = c / rows ;
				int y1 = c - ( x1 * rows ) ;

				graphics.drawLine(x0*scale, y0*scale, x1*scale, y1*scale);
			} ) ;
		} catch( Throwable t ) {
			logger.error( "Failed to print", t ) ;
		}
		rsp.type( "image/png" );
		rsp.header("expires", "0" ) ;
		rsp.header("cache-control", "no-cache" ) ;

		ImageIO.write( img, "png", rsp.raw().getOutputStream() );

		return rsp ;
	}


	public int getPatternId() {
		return wss.getPattern() ;
	}


	@Override
	public void close() {
		spark.Spark.stop() ;
	}
}

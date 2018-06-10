package com.rc.web;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import com.rc.Brain;
import org.ejml.data.DMatrixSparseCSC;
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
	private Object getData(Request req, Response rsp) {
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


	private Object getSynapses( Request req, Response rsp ) throws IOException {

	    logger.info( "Requesting synapse image" ) ;
		int imageType = BufferedImage.TYPE_BYTE_GRAY ;

        DMatrixSparseCSC synapses = brain.getSynapses() ;

        final BufferedImage img = new BufferedImage( synapses.numCols, synapses.numRows, imageType);
		Graphics2D graphics = img.createGraphics();
		graphics.setBackground( Color.BLACK ) ;
		brain.eachNonZero( (r, c, v) -> {
					int p = ( (int) (v * 0x7f ) + 0x80 )  ;
					img.setRGB(r, c, new Color(0, p, p).getRGB());
				}
		) ;
		rsp.type( "image/png" );
		rsp.header("expires", "0" ) ;
		rsp.header("cache-control", "no-cache" ) ;

		ImageIO.write( img, "png", rsp.raw().getOutputStream() );

		return rsp ;
	}



	private Object getGraph( Request req, Response rsp ) throws IOException {

		logger.info( "Requesting graph image" ) ;
		int imageType = BufferedImage.TYPE_USHORT_555_RGB ;

		int rows = brain.getRows()  ;
		int columns = brain.getColumns()  ;
        int scale = 15 ;
		final BufferedImage img = new BufferedImage( columns*scale, rows*scale, imageType ) ;
		Graphics2D graphics = img.createGraphics() ;
		graphics.setBackground( Color.BLACK ) ;
		try {
			brain.eachNonZero( (r, c, v) -> {
				int p = (int) ( v * 0xff ) ;
				if( p<0x5f ){
					graphics.setColor( new Color(0, 0, 0x90 + p) );
				} else if( p <0x7f ) {
					graphics.setColor( new Color(0, p+0x80, 0 ) );
				} else {
					graphics.setColor( new Color(p,0,0) );
				}

				int x0 = r / rows ;
				int y0 = r - ( x0 * rows ) ;
				int x1 = c / rows ;
				int y1 = c - ( x1 * rows ) ;

				graphics.drawLine(x0*scale + (scale / 2), y0*scale+ (scale / 2), x1*scale+ (scale / 2), y1*scale+ (scale / 2));
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

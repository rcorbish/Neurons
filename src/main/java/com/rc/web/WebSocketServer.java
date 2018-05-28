package com.rc.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rc.Brain;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@WebSocket
public class WebSocketServer  {
	final static Logger logger = LoggerFactory.getLogger( WebSocketServer.class ) ;
	
	private final Brain brain ;
	
	private List<Session> sessions = new ArrayList<>() ;
	
	private int following = -1 ;
	private int pattern = 0 ;
	private boolean training = false ;

	public WebSocketServer( Brain brain ) {
		this.brain = brain ;
	}
	
	@OnWebSocketConnect
	public synchronized void connect( Session session )  {
		this.sessions.add( session ) ;	// keep tabs on the remote client
		logger.info( "Opened connection to {} - {} active sessions", session.getRemoteAddress(), sessions.size() ) ;
	}

	@OnWebSocketClose
	public synchronized void close(Session session, int statusCode, String reason) {
		this.sessions.remove( session ) ;
		logger.info( "Closed connection to {} - {} active sessions", session.getRemoteAddress(), sessions.size() ) ;
	}

	@OnWebSocketError
	public synchronized void error(Session session, Throwable error ) {
		this.sessions.remove( session ) ;
		logger.info( "Error {} connection to {} - {} active sessions", error.getLocalizedMessage(), session.getRemoteAddress(), sessions.size() ) ;
	}


	/**
	 * This parses the client message and then hands it off to a client processor. It's 
	 * semi-dumb (which is never good) 
	 * 
	 * @param session
	 * @param message
	 * @throws IOException
	 */
	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		logger.info( "Received {} from {}.", message, session.getRemoteAddress() ) ;
		if( message.startsWith( "follow " ) ) {
			setFollowing( Integer.parseInt( message.substring( "follow ".length() ) ) ) ;
		} else if ( message.startsWith( "pattern " ) ) {
			setPattern( Integer.parseInt( message.substring( "pattern ".length() ) ) ) ;
		} else if ( message.startsWith( "train " ) ) {
			setTrainingMode( message.endsWith("on") ) ;
		} else if ( message.startsWith( "fourier " ) ) {
			setFourierMode( message.endsWith("on") ) ;
		} else if ( message.startsWith( "fourierSpike " ) ) {
			setFourierSpikeMode( message.endsWith("on") ) ;
		}
	}	

	// Leakage of error sessions ?????
	public synchronized void send( String msg ) {
		
		for( Iterator<Session> i=this.sessions.iterator() ; i.hasNext() ; ) {
			Session session = i.next() ;
			if( session.isOpen() ) {
				try {
					session.getRemote().sendString( msg );
				} catch( Exception e ){
					logger.error( "Failed to send msg", e ) ;
					//session.close( 500, "Error sending text" ) ; 
					i.remove() ;
				}
			}
		}
	}

	public int getFollowing() {
		return following;
	}

	public void setFollowing(int following) {
		this.following = following;
	}

	public int getPattern() {
		return pattern;
	}

	public void setPattern(int pattern) {
		this.pattern = pattern;
	}
	
	public void setTrainingMode(boolean training) {
		logger.info( "Training {}", training ? "enabled" : "disabled" ) ;
		brain.setTrain( training ) ;
	}	

	public void setFourierMode(boolean fourier) {
		logger.info( "Fourier mode {}", fourier ? "enabled" : "disabled" ) ;
		brain.setFourierSpike( false ) ;
		brain.setFourier( fourier ) ;
	}
	
	public void setFourierSpikeMode(boolean fourier) {
		logger.info( "Fourier spike mode {}", fourier ? "enabled" : "disabled" ) ;
		brain.setFourier( false ) ;
		brain.setFourierSpike( fourier ) ;
	}	
	
}




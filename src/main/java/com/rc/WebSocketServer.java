package com.rc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
/**
 * One of the more interesting classes, this handles raw messages from the client.
 * It does delegate most work to the private class (below). There is one WebSocketServer
 * per server instance, each client instance has a WebSocketCommandProcessor to 
 * manage itself. We have 2 lists of clients: 1 in client manager and one in clientData. Ideally these
 * need to be merged. TODO all that. The difference is that the client manager handles messages
 * from the web page and the clientData handles messages to - pretty lame
 * 
 * @author richard
 *
 */
@WebSocket
public class WebSocketServer  {
	final static Logger logger = LoggerFactory.getLogger( WebSocketServer.class ) ;
	
	private List<Session> sessions = new ArrayList<>() ;

	@OnWebSocketConnect
	public synchronized void connect( Session session )  {
		this.sessions.add( session ) ;	// keep tabs on the rempote client
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
		logger.debug( "Received {} from {}.", message, session.getRemoteAddress() ) ;
	}	


	public synchronized void send( String msg ) {
		for( Session session : this.sessions ) {
			if( session.isOpen() ) {
				try {
					session.getRemote().sendString( msg );
				} catch( IOException ioe ){
// nothing to do
				}
			} else {
				this.sessions.remove( session ) ;
			}
		}
	}	

}




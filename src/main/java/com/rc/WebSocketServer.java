package com.rc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



@WebSocket
public class WebSocketServer  {
	final static Logger logger = LoggerFactory.getLogger( WebSocketServer.class ) ;
	
	private List<Session> sessions = new ArrayList<>() ;
	
	private int following = -1 ;

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
		}
	}	

	
	public synchronized void send( String msg ) {
		
		for( Session session : this.sessions ) {
			if( session.isOpen() ) {
				try {
					session.getRemote().sendString( msg );
				} catch( Exception e ){
					// nothing to do
				}
//			} else {
//				this.sessions.remove( session ) ;
			}
		}
	}

	public int getFollowing() {
		return following;
	}

	public void setFollowing(int following) {
		this.following = following;
	}	

}




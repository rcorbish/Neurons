package local.rac.custom;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 *  
 * @author richard
 *
 */

public class BrainDataWebSocket extends WebSocketAdapter {
	
    private Session session;
    private final Brain brain ;
    private final ScheduledExecutorService executor ;
    
	public BrainDataWebSocket( Brain brain ) {
		this.brain = brain ;
		executor = Executors.newScheduledThreadPool(1);
	}
	
	// called when the socket connection with the browser is established
    public void onWebSocketConnect(Session session) {
    	System.out.println( "Connected WebSocket !!!!!" ) ;
        this.session = session;
        executor.scheduleAtFixedRate(() ->  sendBrainState(), 0, 75, TimeUnit.MILLISECONDS);
    }
    
    // called when the connection closed
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.println("Connection closed with statusCode=" 
            + statusCode + ", reason=" + reason);
    }
    
    // called in case of an error
    public void onWebSocketError(Throwable error) {
        error.printStackTrace();    
    }
    
    // called when a message received from the browser
    public void onWebSocketText(String message) {
        switch (message) {
            case "start":
                break;
            case "stop":
                this.stop();
                break;
        }
    }
    // sends message to browser
    public void sendBrainState() {
        try {
            if (session.isOpen()) {
                session.getRemote().sendString( brain.getNeuronPotentials().toString() );                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    // closes the socket
    private void stop() {
        try {
            session.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}



package local.rac.custom;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlets.gzip.GzipHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class WebServer {
	private static String REALM 	= "Realm" ; // security realm
	private final Brain brain ;

	/**
	 * Create a server that does not join to the main thread. 
	 * @throws Exception
	 */
	public WebServer( Brain brain ) throws Exception {
		this.brain = brain ;

		// A new server
		Server server = new Server();

		// Get a SSL connector
		ServerConnector connector = getConnector(server) ;
		server.addConnector( connector ) ;

		// Contexts define what URLs go to which handler
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] { getBrainHandler(), getBrainDataHandler(), getImageHandler() } );

		// If suitable request headers are present - we can compress the output
		GzipHandler gzipHandler = new GzipHandler();
		gzipHandler.setHandler( contexts );

		// We always require loggin in - to the web service. Documentation is excluded
		SecurityHandler security = getSecurityHandler() ;
		security.setHandler( gzipHandler );

		// The main handler is the security check, which passes on to compression and then to our pages
		server.setHandler( security ) ;

		// Stop the seever on shutdown of the app
		server.setStopAtShutdown(true);
		server.start();

		//			server.join();
	}

	private Handler getBrainHandler() {
		ContextHandler context = new ContextHandler("/");
		context.setContextPath("/");
		// Data handlers require an extractor - which writes the real output
		context.setHandler( new BrainHandler( brain ) ) ;        
		return context  ;
	}

	@SuppressWarnings("serial")
	public static class BrainDataServlet extends WebSocketServlet {		 
		@Override
		public void configure(WebSocketServletFactory factory) {
			factory.register(BrainDataWebSocket.class);
		}
	}

	private Handler getBrainDataHandler() {
		ContextHandler contextHandler = new ContextHandler("/live-data");
		contextHandler.setAllowNullPathInfo(true); // disable redirect from /ws to /ws/

		final WebSocketCreator webSocketcreator = new WebSocketCreator() {
			public Object createWebSocket(ServletUpgradeRequest request,
					ServletUpgradeResponse response) {
				return new BrainDataWebSocket( brain );
			}
		};

		Handler webSocketHandler = new WebSocketHandler() {
			public void configure(WebSocketServletFactory factory) {
				factory.setCreator(webSocketcreator);
			}
		};

		contextHandler.setHandler(webSocketHandler);
		return contextHandler;
	}


	private Handler getImageHandler() {
		ContextHandler context = new ContextHandler("/img");
		context.setContextPath("/img");
		context.setHandler( new ImageHandler() );
		return context  ;
	}

	/**
	 * Setup a connector for the server to use.This requires http 1.1 - not enough 
	 * client code supports http 2.0 (yet)
	 * 
	 * @param server
	 * @return
	 */
	private ServerConnector getConnector( Server server ) {

		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setOutputBufferSize(32768);

		HttpConfiguration https_config = new HttpConfiguration(http_config);

		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(https_config) ) ;
		http.setPort(8088);
		http.setIdleTimeout(500000);

		return http ;
	}

	/**
	 * Define a security handler. The security handler is a simple properties
	 * file using Basic authentication. The v1 protocol URL (ie. all the REST interfaces) 
	 * require protection. The top-level URL (i.e. documentation) does not need authenticatin
	 * 
	 * @return
	 */
	private SecurityHandler getSecurityHandler() {

		ConstraintSecurityHandler security = new ConstraintSecurityHandler();

		// This constraint requires authentication and in addition that an
		// authenticated user be a member of a given set of roles for
		// authorization purposes.
		Constraint constraintUser = new Constraint();
		constraintUser.setName("auth");
		constraintUser.setAuthenticate(true);
		constraintUser.setRoles(new String[] { "user" });

		// Binds a url pattern with the previously created constraint. The roles
		// for this constraing mapping are mined from the Constraint itself
		// although methods exist to declare and bind roles separately as well.
		ConstraintMapping mappingUser = new ConstraintMapping();
		mappingUser.setPathSpec("/admin/*");
		mappingUser.setConstraint(constraintUser);

		Constraint constraintSupport = new Constraint();
		constraintSupport.setName("auth");
		constraintSupport.setAuthenticate(true);
		constraintSupport.setRoles(new String[] { "support" });

		ConstraintMapping mappingSupport = new ConstraintMapping();
		mappingSupport.setPathSpec("/support");
		mappingSupport.setConstraint(constraintSupport);

		// First you see the constraint mapping being applied to the handler as
		// a singleton list, however you can passing in as many security
		// constraint mappings as you like so long as they follow the mapping
		// requirements of the servlet api. Next we set a BasicAuthenticator
		// instance which is the object that actually checks the credentials
		// followed by the LoginService which is the store of known users, etc.
		List<ConstraintMapping> constraints = new ArrayList<>() ;
		constraints.add( mappingUser ) ;
		constraints.add( mappingSupport ) ;

		security.setConstraintMappings( constraints );
		security.setAuthenticator(new BasicAuthenticator());
		security.setLoginService( new HashLoginService( REALM, "login.properties" )  ) ;

		return security;
	}
}

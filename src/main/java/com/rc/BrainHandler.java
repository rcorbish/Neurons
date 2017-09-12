package com.rc ;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * This will pull a jpeg from directory and return its data as a raw stream.
 * This only supports jpeg files at present.
 * 
 * @author richard
 *
 */
public class BrainHandler extends AbstractHandler {
	
	final private Brain brain ;
	
	public BrainHandler( Brain brain ) {
		this.brain = brain ;
	}
	
	@Override
	public void handle(String arg0, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		
		// If file is OK - send it back as a stream.

		try {
			PrintWriter pw = response.getWriter() ;
			if( arg0.equals("/") ) {
				response.setStatus( HttpServletResponse.SC_OK );	
				response.setContentType("text/html");
				printBrainPage(pw);
			} else if (arg0.equals("/data") ) {
				response.setStatus( HttpServletResponse.SC_OK );	
				response.setContentType("application/json");
				printBrainData(pw ) ;
			} else {
				response.setStatus( HttpServletResponse.SC_NOT_FOUND );	
			}
			
		} finally {
			response.flushBuffer();
		}
		baseRequest.setHandled( true ) ;		
	}

	protected void printBrainData( PrintWriter pw ) {
		
		CharSequence cs = brain.toJson() ;
		pw.println( cs.toString() ) ;
	}
	
	
	protected void printBrainPage( PrintWriter pw ) {
		File brainHtmlFile = new File( "src/main/resources/html/brain.html" ) ;
		try ( FileReader fr = new FileReader( brainHtmlFile ) ) {
			char[] buf = new char[1024] ;
			
			int n ; 
			do { 
				n = fr.read(buf);
				if( n > 0 ) {
					pw.write(buf, 0, n );
				}
			} while( n > 0 ) ;
		} catch( IOException ioe ) {
			pw.write( "Error:" + ioe.getMessage() ) ;
		}
	}
}



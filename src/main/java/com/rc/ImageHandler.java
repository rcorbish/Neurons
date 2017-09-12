package com.rc ;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
public class ImageHandler extends AbstractHandler {

	@Override
	public void handle(String arg0, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		
		// Check the file exists - if not 404 ...
		File imgFile = new File( "." + baseRequest.getPathInfo() ) ;
		if( !imgFile.canRead() ) {
			response.setStatus( HttpServletResponse.SC_NOT_FOUND );	
		}
		// If file is OK - send it back as a stream.
		response.setStatus( HttpServletResponse.SC_OK );	
		response.setContentType("image/jpeg");

		try (  InputStream is = new FileInputStream( imgFile ) ) {		
			OutputStream os = response.getOutputStream() ;
			int n ;
			byte [] buf = new byte[10000] ;
			while( (n = is.read(buf)) > 0  ) os.write(buf,0,n);
			
		} finally {
			response.flushBuffer();
		}
		baseRequest.setHandled( true ) ;		
	}
}

package com.rc ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

import com.rc.web.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


public class Main {
	final static Logger log = LoggerFactory.getLogger( Main.class ) ;

	final static Random rng = new Random( 660 );
	
	static double CONNECTION_DENSITY = 0.2 ;
	static int POPULATION = 40 ;
	static int EPOCHS = 10 ;
	static double LIFESPAN = 1.0 ;
	static double TICK_PERIOD = 2e-4 ;   // each clock tick in seconds
	static double MUTATION = 0.01 ;
	static long DELAY_INTERVAL  = 25 ;
	
	public static int FIXED_PARAMS[]  ;
	
	static double [][] TestPatterns = {
		{ 0.5, 0.3, 0.6, 0.1, 0.9, 0.9 },
		{ 0.9, 0.9, 0.9, 0.1, 0.1, 0.1 },
		{ 0.9, 0.5, 0.1, 0.1, 0.5, 0.9 },
		{ 0.1, 0.5, 0.9, 0.9, 0.5, 0.1 },
		{ 0.9, 0.9, 0.9, 0.9, 0.5, 0.1 },
		{ 0.1, 0.5, 0.9, 0.9, 0.9, 0.9 },
		{ 0.5, 0.5, 0.5, 0.5, 0.5, 0.5 },
		{ 0.1, 0.1, 0.1, 0.1, 0.1, 0.1 },
		{ 0.9, 0.9, 0.9, 0.9, 0.9, 0.9 }
	} ;

	public static void main(String[] args) {
		try {
			
			OptionParser parser = new OptionParser();
			
			parser.acceptsAll( asList("f", "file") , "The parameters in json format" ).withRequiredArg().ofType( String.class ) ;
			parser.acceptsAll( asList("e", "evolve") , "Whether to run the evolution step" ) ;
			parser.acceptsAll( asList("p", "period") , "Each clock step is this long (uS)" ).withRequiredArg().ofType( Integer.class ) ;
			parser.acceptsAll( asList("d", "density") , "Synapse connection density 0-1" ).withRequiredArg().ofType( Double.class ) ;
			parser.acceptsAll( asList("u", "update-delay" ) , "Interval between updates 0-200 mS" ).withRequiredArg().ofType( Long.class ) ; 
			parser.accepts( "epochs" , "Number of epochs to run" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "clear" , "Delete existing parameters" ) ; 
			parser.accepts( "train" , "Train the network" ) ; 
			parser.accepts( "lifespan" , "Lifespan of each simulation" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "population" , "Number of brains in the population" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "mutation" , "Mutation amount 0.0 - 1.0" ).withRequiredArg().ofType( Double.class ) ; 
			parser.nonOptions( "Network dimensions ( up to 3 ) (e.g. 3 4, 2 2 2 )" ).ofType( Integer.class ) ; 
			parser.accepts( "help", "This help" ).forHelp();
			
	        OptionSet options = parser.parse( args ) ;
	
			if( options.has( "help")  ) {
				try {
					parser.printHelpOn( System.out );
					System.exit(1) ;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

	        if( options.has( "lifespan" ) ) 	{ LIFESPAN = (int) options.valueOf("lifespan") ; }
	        if( options.has( "population" ) ) 	{ POPULATION = (int) options.valueOf("population") ; }
	        if( options.has( "epochs" ) ) 		{ EPOCHS = (int) options.valueOf("epochs") ; }
	        if( options.has( "mutation" ) ) 	{ MUTATION = (double) options.valueOf("mutation") ; }
	        if( options.has( "update-delay" ) ) { DELAY_INTERVAL = (long) options.valueOf("update-delay") ; }
	        if( options.has( "period" ) ) 		{ TICK_PERIOD = (int) options.valueOf("period") ; }
	        if( options.has( "density" ) ) 		{ CONNECTION_DENSITY = (double) options.valueOf("density") ; }
	        boolean clearFile = options.has("clear") ;
	        boolean evolve    = options.has("evolve") ;
	        boolean train	  = options.has("train") ;
	        
	        String parameterFile = options.has("f") ? options.valueOf( "f" ).toString() : null ;

			int dims[] ;
			List<?> dimArgs = options.nonOptionArguments() ;
			if( dimArgs.size() > 0 ) {
				dims = new int[ 2 ] ;
				dims[0] = Integer.parseInt( dimArgs.get(0).toString() ) ;
				if( dimArgs.size() > 1 ) {
					dims[1] = Integer.parseInt( dimArgs.get(1).toString() ) ;
				} else {
					dims[1] = dims[0] ;
				}
			} else {
				dims = new int[]{ 30, 80 } ;	// default if no size given
			}
			FIXED_PARAMS = new int[ dims.length + 1 ] ;
			FIXED_PARAMS[0] = 0 ;
			
			StringJoiner sj = new StringJoiner( ", " ) ;
			for( int i=0 ; i<dims.length ; i++ ) {
				sj.add( String.valueOf(dims[i]) ) ;
				FIXED_PARAMS[i+1] = i+1 ;
			}
			log.info("Layers        : {}", sj ) ;
			log.info("Delay         : {}", DELAY_INTERVAL ) ;
			
			boolean fileExists = false ;
			if( parameterFile !=null ) {
				File f = new File( parameterFile ) ;
				fileExists = f.canRead() ;
			}
			
			Brain brain ;
			brain = new Brain( TICK_PERIOD, CONNECTION_DENSITY, 6, 10, dims[0], dims[1] ) ;
			/*
			if( fileExists && !clearFile ) {
				brain = Brain.load( TICK_PERIOD, parameterFile ) ;
			} else {
				brain = new Brain( TICK_PERIOD, 8, 10, 5, 5 ) ;
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}
			
			if( evolve ) {
				log.info("Epochs        : {}", EPOCHS );
				log.info("Population    : {}", POPULATION );
				log.info("Lifespan      : {}", LIFESPAN );
				log.info("Mutation Rate : {}", MUTATION );
				final Evolution evolution = new Evolution(TICK_PERIOD, LIFESPAN, MUTATION, EPOCHS, POPULATION ) ;
				brain = evolution.evolve( TestPatterns, TICK_PERIOD, dims ) ;
				
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}
			*/
			brain.setTrain( train ) ;
			
			@SuppressWarnings("resource")
			Monitor m = new Monitor( brain ) ;
			m.start();
			
			double inputs[] = new double[ brain.getNumInputs() ] ;

			int patternCount = 0 ;
			int patternIndex = 0 ;
			double testPattern[] = null ;
			long lastSentTime = 0 ;
			for( ; ; ) {
				// Display a pattern for a few cycles, then change
				patternCount-- ;
				if( patternCount<0) {
					patternCount = 5 ;

					//patternIndex = rng.nextInt(TestPatterns.length) ;
					patternIndex = brain.isTrain() ? 
							rng.nextInt(TestPatterns.length) : 
							m.getPatternId() ;
					
					if( patternIndex >=0 && patternIndex < TestPatterns.length ) {
						testPattern = TestPatterns[ patternIndex ] ;
					}

					for( int i=0 ; i<inputs.length ; i++ ) {
						inputs[i] = testPattern[i] ;
					}
				}
				
				// Do one clock period - full network traversal
				brain.step( inputs ) ;
				
				// If necessary - update history for GUI
				brain.follow() ;
				
				brain.train() ;

				// -------------------------------------------------
				// If we're training, 
				// 	- send periodic updates
				// 	- operate machine at full speed
				// If we're not training 
				// 	- send info each step
				// 	- operate machine slowly
				if( brain.isTrain() ) {
					long sinceLastMessage = System.currentTimeMillis() - lastSentTime ;
					if( sinceLastMessage > DELAY_INTERVAL ) {
						lastSentTime = System.currentTimeMillis() ;
						m.sendBrainData( brain.clock() ) ; 
					}
				} else {
					lastSentTime = System.currentTimeMillis() ;
					m.sendBrainData( brain.clock() ) ; 
					if( DELAY_INTERVAL>0 ) {
						Thread.sleep( DELAY_INTERVAL ) ;
					}
				}
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}
		
	private static List<String> asList( String ... strings ) {
		List<String> rc = new ArrayList<String>() ;
		for( String string : strings ) rc.add( string ) ;
		return rc ;
	}
}

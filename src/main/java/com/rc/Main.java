package com.rc ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


public class Main {
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;

	final static Random rng = new Random( 660 );
	
	static int POPULATION = 40 ;
	static int EPOCHS = 10 ;
	static double LIFESPAN = 1.0 ;
	static double TICK_PERIOD = 1e-4 ;   // each clock tick in seconds - default 100uS
	static double MUTATION = 0.01 ;
	static long DELAY_INTERVAL  = 150 ;
	
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
	        boolean clearFile = options.has("clear") ;
	        boolean evolve    = options.has("evolve") ;
	        boolean train	  = options.has("train") ;
	        
	        String parameterFile = options.has("f") ? options.valueOf( "f" ).toString() : null ;

			int dims[] ;
			List<?> dimArgs = options.nonOptionArguments() ;
			if( dimArgs.size() > 0 ) {
				dims = new int[ dimArgs.size() ] ;
				for( int i=0 ; i<dims.length ; i++ ) {
					dims[i] =  Integer.parseInt( dimArgs.get(i).toString() ) ;
				}
			} else {
				dims = new int[]{ 3, 3 } ;	// default if no size given
			}
			FIXED_PARAMS = new int[ dims.length + 1 ] ;
			FIXED_PARAMS[0] = 0 ;
			
			StringJoiner sj = new StringJoiner( ", " ) ;
			for( int i=0 ; i<dims.length ; i++ ) {
				sj.add( String.valueOf(dims[i]) ) ;
				FIXED_PARAMS[i+1] = i+1 ;
			}
			logger.info("Layers        : {}", sj ) ;
			logger.info("Delay         : {}", DELAY_INTERVAL ) ;
			
			boolean fileExists = false ;
			if( parameterFile !=null ) {
				File f = new File( parameterFile ) ;
				fileExists = f.canRead() ;
			}
			
			Brain brain ;
			if( fileExists && !clearFile ) {
				brain = Brain.load( TICK_PERIOD, parameterFile ) ;
			} else {
				brain = new Brain( TICK_PERIOD, dims ) ;
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}

			if( evolve ) {
				logger.info("Epochs        : {}", EPOCHS );
				logger.info("Population    : {}", POPULATION );
				logger.info("Lifespan      : {}", LIFESPAN );
				logger.info("Mutation Rate : {}", MUTATION );
				final Evolution evolution = new Evolution(TICK_PERIOD, LIFESPAN, MUTATION, EPOCHS, POPULATION ) ;
				brain = evolution.evolve( TestPatterns, TICK_PERIOD, dims ) ;
				
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}

			@SuppressWarnings("resource")
			Monitor m = new Monitor( brain ) ;
			m.start();
			double inputs[] = new double[ dims[0] ] ;

			int patternCount = 0 ;
			int patternIndex = 0 ;
			double testPattern[] = null ;
			long lastSentTime = 0 ;
			for( ; ; ) {

				patternCount-- ;
				if( patternCount<0) {
					patternCount = 1 ;
					//patternIndex = rng.nextInt(TestPatterns.length) ;
					patternIndex = m.getPatternId() ;
					
					if( patternIndex >=0 && patternIndex < TestPatterns.length ) {
						testPattern = TestPatterns[ patternIndex ] ;
					}

					for( int i=0 ; i<inputs.length ; i++ ) {
						inputs[i] =  testPattern[i] ;
					}
				}

				brain.step( inputs ) ;
				brain.follow() ;
				if( train ) {
					brain.train() ;
				}

				long deltaTime = System.currentTimeMillis() - lastSentTime ;
				if( !train || deltaTime > DELAY_INTERVAL ) {
					lastSentTime = System.currentTimeMillis() ;
					m.sendBrainData( brain.clock() ) ; 
				}
				if( DELAY_INTERVAL>0 ) {
					Thread.sleep( DELAY_INTERVAL ) ;
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

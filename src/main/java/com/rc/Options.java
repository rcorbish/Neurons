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


public class Options {
	final static Logger log = LoggerFactory.getLogger( Options.class ) ;

	public static int POPULATION       = 40 ;
	public static int EPOCHS           = 10 ;
	public static int BATCH_SIZE       = 300 ;
	public static int SIMULATIONS      = 100 ;
	public static double TICK_PERIOD   = 2e-4 ;   // each clock tick in seconds
	public static double MUTATION      = 0.01 ;
	public static long DELAY_INTERVAL  = 25 ; 

	public static boolean train 		= false ;
	public static boolean evolve 		= false ;
	public static boolean clearFile 	= false ;
	public static String  parameterFile = null ;
	public static int 	  dims[] ;
	
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

	public static void parseCommandLine(String[] args) {
		
		OptionParser parser = new OptionParser();
		
		parser.acceptsAll( asList("f", "file") , "The parameters in json format" ).withRequiredArg().ofType( String.class ) ;
		parser.acceptsAll( asList("e", "evolve") , "Whether to run the evolution step" ) ;
		parser.acceptsAll( asList("p", "period") , "Each clock step is this long (uS)" ).withRequiredArg().ofType( Integer.class ) ;
		parser.acceptsAll( asList("d", "density") , "Synapse connection density 0-1" ).withRequiredArg().ofType( Double.class ) ;
		parser.acceptsAll( asList("u", "update-delay" ) , "Interval between updates 0-200 mS" ).withRequiredArg().ofType( Long.class ) ; 
		parser.accepts( "epochs" , "Number of epochs to run" ).withRequiredArg().ofType( Integer.class ) ; 
		parser.accepts( "clear" , "Delete existing parameters" ) ; 
		parser.accepts( "train" , "Train the network" ) ;
		parser.accepts( "simulations" , "Number of simulations to run" ).withRequiredArg().ofType( Integer.class ) ;
		parser.accepts( "batch" , "Batch size per simulation" ).withRequiredArg().ofType( Integer.class ) ;
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

		if( options.has( "simulations" ) ) 	{ SIMULATIONS= (int) options.valueOf("simulations") ; }
		if( options.has( "population" ) ) 	{ POPULATION = (int) options.valueOf("population") ; }
		if( options.has( "epochs" ) ) 		{ EPOCHS = (int) options.valueOf("epochs") ; }
		if( options.has( "batch" ) ) 			{ BATCH_SIZE = (int) options.valueOf("batch") ; }
		if( options.has( "mutation" ) ) 		{ MUTATION = (double) options.valueOf("mutation") ; }
		if( options.has( "update-delay" ) ) 	{ DELAY_INTERVAL = (long) options.valueOf("update-delay") ; }
		if( options.has( "period" ) ) 		{ TICK_PERIOD = (int) options.valueOf("period") ; }

		clearFile = options.has("clear") ;
		evolve    = options.has("evolve") ;
		train	  = options.has("train") ;
		
		parameterFile = options.has("f") ? options.valueOf( "f" ).toString() : null ;

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
		

		if( evolve ) {
			log.info("Epochs        : {}", EPOCHS );
			log.info("Population    : {}", POPULATION );
			log.info("Batch Size    : {}", BATCH_SIZE );
			log.info("Simulations   : {}", SIMULATIONS );
			log.info("Mutation Rate : {}", MUTATION );				
		}
	}
		
	private static List<String> asList( String ... strings ) {
		List<String> rc = new ArrayList<String>() ;
		for( String string : strings ) rc.add( string ) ;
		return rc ;
	}
}

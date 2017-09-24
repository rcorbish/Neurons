package com.rc ;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.ConcurrentArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


public class Main {
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;

	final static Random rng = new Random( 660 );
	final static int INPUT_COUNT = 5 ;
	final static int OUTPUT_COUNT = 5 ;
	final static int POPULATION = 5_000 ;
	final static int EPOCHS = 200 ;
	final static int SIMULATIONS = 2000 ;
	
	public static void main(String[] args) {
		try {
			
			OptionParser parser = new OptionParser( "f::e" );
	        OptionSet options = parser.parse( args ) ;
	        
			String parameterFile = options.has("f") ? options.valueOf( "f" ).toString() : null ;
			logger.info("Using file {}", parameterFile ) ;

			List<?> dimArgs = options.nonOptionArguments() ;
			
			int dims[] = new int[ dimArgs.size() ] ;
			for( int i=0 ; i<dims.length ; i++ ) {
				dims[i] =  Integer.parseInt( dimArgs.get(i).toString() ) ;
			}

			StringJoiner sj = new StringJoiner( " x " ) ;
			for( int d : dims ) {
				sj.add( String.valueOf(d) ) ;
			}
			logger.info("Network size: {}", sj );

			boolean evolve = options.has( "e" ) ;
			
			BrainParameters parameters = new BrainParameters() ;
			parameters.numInputs = INPUT_COUNT ;
			parameters.numOutputs = OUTPUT_COUNT ;
			parameters.inhibitorRatio = .5 ;
			parameters.dimensions = dims ;
			parameters.spikeThreshold = 0.6 ;
			parameters.spikeProfile = new double[]{ 0, 0.5, 1, 0.4, 0, -0.1, -0.17, -0.16, -0.15 } ;
			parameters.restingPotential = -.10 ;
			
			boolean fileExists = false ;
			if( parameterFile !=null ) {
				File f = new File( parameterFile ) ;
				fileExists = f.canRead() ;
			}
			Brain brain = fileExists ? 
							Brain.load( parameterFile, dims ) :
							new Brain(parameters, dims ) ;
			if( evolve ) {
				brain = evolve( dims ) ;
				
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}

			Monitor m = new Monitor( brain ) ;
			m.start();
			double inputs[] = new double[parameters.numInputs] ;

			int clk = 0 ;
			for( ; ; ) {
				clk++ ;
				for( int i=0 ; i<inputs.length ; i++ ) {
					inputs[i] =  rng.nextInt( 1+(clk % 4) )==0 ? 1 : 0 ;
					inputs[i] =  Math.cos(i * clk / Math.PI ) ;
				}
				// inputs[0] = 1 / ( (clk % 10) + 1 ) ;
				// inputs[0] = Math.abs( Math.sin( clk / Math.PI ) ) ;
				// inputs[1] = Math.cos( 3 * clk / Math.PI ) ;
				// inputs[1] *= inputs[1] + rng.nextDouble()/10;
				brain.step( inputs ) ;
				brain.updateScores() ;
				m.sendBrainData(); 
				Thread.sleep(150);
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}
	
	public static Brain evolve( final int ... dims ) throws Exception {
		logger.info( "Evolution starts..." ) ;
		
		BrainData brainData[] = new BrainData[ POPULATION ] ;
		
		for( int i=0 ; i<brainData.length ; i++ ) {
			BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
			for( int j=0 ; j<BrainParameters.GENOME_SIZE ; j++ ) {
				if( rng.nextBoolean() ) {
					bs.set(j) ; 
				}
			}
			brainData[i] = new BrainData( bs, dims ) ;
		}

		logger.info( "Population created." ) ;

		double inputs[] = new double[ INPUT_COUNT ] ;
		
		ExecutorService tpool = Executors.newFixedThreadPool(6) ;
		
		for( int e=0 ; e<EPOCHS ; e++ ) {			
			for( int s=0 ; s<SIMULATIONS ; s++ ) {
				for( int i=0 ; i<inputs.length ; i++ ) {
					inputs[i] = rng.nextDouble() ;
				}
				final CountDownLatch cdl = new CountDownLatch( brainData.length ) ;
				for( int i=0 ; i<brainData.length ; i++ ) {
					final Brain brain = brainData[i].brain ;
					tpool.submit( new Thread() {
						public void run() {
							brain.step( inputs ) ;
							brain.updateScores() ;
							cdl.countDown();
							//logger.info( "CDL: {}", cdl.getCount() ) ; 
						}
					} ) ;
				}
				cdl.await();  // wait for all brains to complete one step and scoring
			}
			
			for( int i=0 ; i<brainData.length ; i++ ) {
				brainData[i].score = brainData[i].brain.getScore() ;
			}
			Arrays.sort( brainData ) ;
			int survivingIndex = brainData.length ;
			
			int numNewBrains = brainData.length / 2 ;
			CountDownLatch cdl = new CountDownLatch(numNewBrains) ;
			
			final Queue<BrainData> newBrains = new ConcurrentArrayQueue<>() ;
			
			for( int i=0 ; i<numNewBrains ; i++ ) {
				int ix1 = rng.nextInt( survivingIndex ) ;
				int ix2 = rng.nextInt( survivingIndex ) ;
				
				// Make sure ix1 is higher score than ix2 ( brainData is sorted )
				if( ix1 > ix2 ) {
					int tmp = ix2 ;
					ix2 = ix1 ;
					ix1 = tmp ;
				}
				BitSet p1 = brainData[ ix1 ].genome;
				BitSet p2 = brainData[ ix2 ].genome;
				
				final BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
				
				// Inheritance
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					bs.set( b,  rng.nextInt(2)==0 ? p2.get(b) : p1.get(b) ) ;
				}
				// Mutation = x%
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					if( rng.nextDouble() < 0.15 ) {
						bs.set( b,  rng.nextBoolean()  ) ;
					}
				}
				Runnable t = new Runnable() {
					public void run() {
						try {
							BrainData bd = new BrainData( bs, dims ) ;
							newBrains.add( bd ) ;
						} catch( Throwable t ) {
							logger.warn( "WTF - brainless.", t ) ;
						}
						cdl.countDown();
					}
				} ;
				tpool.submit( t ) ;
			}
			cdl.await() ; 
			
			for( int i=0 ; i<newBrains.size() ; i++ ) {
				BrainData bd = newBrains.remove() ;
				brainData[brainData.length - i - 1] = bd ;
				
			}
			
			logger.info( "Epoch {} - best score {}", e, brainData[0].brain.getScore() ) ;
		}
		
		tpool.shutdown();
		boolean oops = tpool.awaitTermination( 10, TimeUnit.MINUTES ) ;
		if( !oops  ) {
			logger.warn( "OMG - too late "); 
		}
		
		BrainParameters bp = new BrainParameters( brainData[0].genome ) ;
		bp.numInputs = Main.INPUT_COUNT ;
		bp.numOutputs = Main.OUTPUT_COUNT ;
		logger.info( "Best bp = {}", bp ) ;
		return brainData[0].brain ;
	}
}

class BrainData implements Comparable<BrainData>{
	BitSet genome ;
	Brain brain ;
	double score ;
	
	public BrainData( BitSet genome, int ...dims ) {
		this.genome = genome ;
		BrainParameters bp = new BrainParameters( genome ) ;
		bp.numInputs = Main.INPUT_COUNT ;
		bp.numOutputs = Main.OUTPUT_COUNT ;
		this.brain = new Brain( bp, dims ) ;
	}
	@Override
	public int compareTo(BrainData o) {
		double diff = o.score - brain.getScore()  ;
		return diff>0 ? 1 : ( diff==0 ? 0 : -1 ) ;
	}
	
}
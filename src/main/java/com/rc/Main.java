package com.rc ;

import java.io.File;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;

	final static Random rng = new Random( 660 );
	final static int INPUT_COUNT = 5 ;
	final static int OUTPUT_COUNT = 5 ;
	final static int POPULATION = 100 ;
	final static int EPOCHS = 50 ;
	final static int SIMULATIONS = 1_500 ;
	
	public static void main(String[] args) {
		try {
			
			String parameterFile = args.length > 0 ? args[0] : null ;
			int xdim = args.length > 1 ? Integer.parseInt(args[1]) : INPUT_COUNT ;
			int ydim = args.length > 2 ? Integer.parseInt(args[2]) : OUTPUT_COUNT ;
			logger.info("Using file {}", parameterFile ) ;
			logger.info("Network size: {} x {}", xdim, ydim );

			BrainParameters parameters = new BrainParameters() ;
			parameters.numInputs = INPUT_COUNT ;
			parameters.numOutputs = OUTPUT_COUNT ;
			parameters.connectivityFactor = 0.75 ;
			parameters.inhibitorRatio = .5 ;
			parameters.dimensions = new int[]{ xdim, ydim } ;
			parameters.spikeThreshold = 0.6 ;
			parameters.transmissionFactor = 1 ;
			parameters.spikeProfile = new double[]{ 0, 0.5, 1, 0.4, 0, -0.1, -0.17, -0.16, -0.15 } ;
			parameters.restingPotential = -.10 ;
			
			boolean fileExists = false ;
			if( parameterFile !=null ) {
				File f = new File( parameterFile ) ;
				fileExists = f.canRead() ;
			}
			Brain brain = fileExists ? 
							Brain.load( parameterFile, xdim, ydim ) :
							new Brain(parameters, xdim, ydim ) ; 
			brain = evolve( xdim, ydim ) ;
			if( parameterFile != null ) {
			 	brain.save( parameterFile ) ;
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
	
	public static Brain evolve( final int xdim, final int ydim ) throws Exception {
		BrainData brainData[] = new BrainData[ POPULATION ] ;
		
		for( int i=0 ; i<brainData.length ; i++ ) {
			BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
			for( int j=0 ; j<BrainParameters.GENOME_SIZE ; j++ ) {
				if( rng.nextBoolean() ) {
					bs.set(j) ; 
				}
			}
			brainData[i] = new BrainData( bs, xdim, ydim ) ;
		}
		
		double inputs[] = new double[ INPUT_COUNT ] ;
		
		logger.info( "Runing epochs ..."  );
		
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
			for( int i=survivingIndex / 2 ; i<brainData.length ; i++ ) {
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
				
				BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
				
				// Inheritance
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					bs.set( b,  rng.nextInt(2)==0 ? p2.get(b) : p1.get(b) ) ;
				}
				// Mutation = 8%
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					if( rng.nextDouble() < 0.08 ) {
						bs.set( b,  rng.nextBoolean()  ) ;
					}
				}
				
				brainData[i] = new BrainData( bs, xdim, ydim ) ;
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
	
	public BrainData( BitSet genome, int xdim, int ydim ) {
		this.genome = genome ;
		BrainParameters bp = new BrainParameters( genome ) ;
		bp.numInputs = Main.INPUT_COUNT ;
		bp.numOutputs = Main.OUTPUT_COUNT ;
		this.brain = new Brain( bp, xdim, ydim ) ;
	}
	@Override
	public int compareTo(BrainData o) {
		double diff = o.score - brain.getScore()  ;
		return diff>0 ? 1 : ( diff==0 ? 0 : -1 ) ;
	}
	
}
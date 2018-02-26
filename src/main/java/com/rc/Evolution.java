package com.rc ;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Evolution { 
	final static Logger logger = LoggerFactory.getLogger( Evolution.class ) ;

	final static Random rng = new Random( 660 );

	final double clockTick ;
	final double mutationRate ;
	final double lifespan ;
	final int    population ;
	final int 	 generations ;

	public Evolution( 
			double  clockTick,
			double  lifespan ,
			double  mutationRate ,
			int 	generations ,
			int     population 
			) {
		this.clockTick = clockTick ;
		this.mutationRate = mutationRate ;
		this.generations = generations ;
		this.lifespan = lifespan ;
		this.population = population ;
	}	


	public Brain evolve( double patterns[][], double tick, final int ... layers ) throws Exception {
		logger.info( "Evolution starts..." ) ;

		BrainData brainData[] = new BrainData[ population ] ;

		for( int i=0 ; i<brainData.length ; i++ ) {
			brainData[i] = new BrainData( new Brain( tick, layers ) ) ;
		}

		logger.info( "Population created." ) ;

		double inputs[] = new double[ layers[0] ] ;

		ExecutorService tpool = Executors.newFixedThreadPool(7) ;

		for( int g=0 ; g<generations ; g++ ) {	// A3 = run each independent brain for 1 generation 
			int cp = 1 ;	// pattern count - display this many times
			int p = 0 ;		// pattern to display
			for( double clock=0 ; clock<lifespan ; clock += clockTick ) {  // each simulation lasts ...
				logger.debug( "Timestep {}", clock ) ;
				// A1 = rotate through patterns
				cp-- ;
				if( cp <= 0 ) {
					for( int i=0 ; i<patterns[p].length ; i++ ) {
						inputs[i] = patterns[p][i] ;
					}
					p++ ;
					if( p>=patterns.length ) {
						p = 0 ;
					}
					cp = 100 ;
				}
				// A1

				// A2 = threads for 1 step & 1 train
				final CountDownLatch cdl = new CountDownLatch( brainData.length ) ;
				for( int i=0 ; i<brainData.length ; i++ ) {
					final Brain brain = brainData[i].brain ;
					tpool.submit( new Thread() {
						public void run() {
							try {
								brain.step( inputs ) ;
								brain.train() ;
							} catch( Throwable t ) {
								logger.error( "Failed to step", t ) ;
							} finally {
								cdl.countDown();
							}
						}
					} ) ;
				}
				cdl.await();  // wait for all brains to complete one step and scoring
				// A2
			} // end of the lifetimes ( clock ) 

			logger.info( "Calculating scores" ) ;

			// Now feed each pattern to every brain and then test its 
			// output vs. expected
			for( int i=0 ; i<brainData.length ; i++ ) {
				brainData[i].resetScore(); 
			}

			// Now test each pattern ...
			for( int pn=0 ; pn<patterns.length ; pn++ ) {
				// set the inputs to the pattern
				for( int i=0 ; i<patterns[i].length ; i++ ) {
					inputs[i] = patterns[p][i] ;
				}
				// Then step all brains through a pattern
				for( int i=0 ; i<brainData.length ; i++ ) {
					for( int j=0 ; j<100 ; j++ ) {
						brainData[i].brain.step( inputs ) ;
					}
					brainData[i].updateScore( p ) ;
				}
			}
			
			Arrays.sort( brainData ) ;

			// Euthanize the lower ( weakest scores ) half 
			int numNewBrains = brainData.length / 2 ;

			for( int i=0 ; i<numNewBrains ; i++ ) {
				// Prefer smarter brains 
				// if we can choose a high score ( from beginning of the sorted population )
				int ix1 = rng.nextInt( Math.max(numNewBrains/4, i) ) ;
				int ix2 = rng.nextInt( Math.max(numNewBrains/4, i) ) ;

				Genome p1 = brainData[ ix1 ].genome() ; 
				Genome p2 = brainData[ ix2 ].genome() ;

				Genome child = new Genome( p1, p2, mutationRate, Main.FIXED_PARAMS ) ;
				
				brainData[brainData.length-i-1] = new BrainData( new Brain( tick, child ) ) ;
			}	
			logger.info( "Generation {} - best score {}", g, brainData[0].score ) ;
		} // A3 - all generations have been tested

		tpool.shutdown();
		boolean oops = tpool.awaitTermination( 10, TimeUnit.MINUTES ) ;
		if( !oops  ) {
			logger.warn( "OMG - too late "); 
		}

		BrainData bd =  brainData[0] ;
		logger.info( "Best score = {}", bd.score ) ;
		return bd.brain ;
	}	


	class BrainData implements Comparable<BrainData>{
		final Brain brain ;
		double score ;

		public BrainData( Brain b ) {
			this.brain = b ;
		}

		public Genome genome() {
			return brain.toGenome() ;
		}

		public void resetScore() {
			score = 0 ;
		}
		
		public void updateScore( int p ) {
			score += brain.getScore(p) ;
			logger.debug( "Scored {}", score ) ;
		}

		
		// Sorts large to small ( inverse to usual order )
		@Override
		public int compareTo(BrainData o) {		
			logger.debug( "Comparing {} with {}", score, o.score ) ;
			return (score > o.score ) ? -1 : ( (score < o.score) ? 1 : 0 ) ;
		}
	}
}
package com.rc ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Neuron  {

	final static Logger log = LoggerFactory.getLogger( Neuron.class ) ;

	private final static int NUM_SPIKES_TO_RECORD = 25 ;
	
	private final static Random rng = new Random()  ;
	private final static int GENOME_INDEX_THRESHOLD = 0 ;
	private final static int GENOME_INDEX_RESTING = 1 ;
	private final static int GENOME_INDEX_DECAY = 2 ;
	private final static int GENOME_INDEX_LEARNING_RATE = 3 ;
	private final static int GENOME_INDEX_SPIKE_VALUE = 4 ;
	private final static int GENOME_INDEX_LEARNING_WINDOW = 5 ;
	private final static int GENOME_INDEX_REFRACTORY_DELAY = 6 ;
	private final static int GENOME_INDEX_THRESHOLD_LEARNING_RATE = 7 ;
	public  final static int GENOME_SIZE = 8 ;

	// These are transient state data
	protected 		double 		currentPotential ;
	protected 		boolean		isSpiking ;
	private  		double 		lastSpikeTime ;				// when did we spike last
	private  		double 		refractoryPeriodStart ;		// how to calc refractory factor
	private 		double		refractoryFactor ;
	
	// The following items are held in the genome
	private final int 		id ;
	private final double 	learningRate ;
	private final double 	thresholdLearningRate ;
	private       double 	threshold  ;
	private final double 	decay ;
	private final double 	spikeValue ;
	private final double 	refractoryDelay ;	
	private final double 	restingPotential ;
	private final double 	learningWindow ;

	private 	  double	frequency ;
	private final double 	lastSpikes[] ;
	int lastSpikeIndex ;

	public Neuron( int id) {
		this.restingPotential = 0 ; 			// o/p potential when not spiking
		this.threshold = 0.8 ; 					// spike triggered when internal potential hit this value
		this.decay = 0.20 ; 					// current potential *= ( 1 - decay )
		this.learningRate = 0.003 ;				// how fast to adjust weights
		this.spikeValue = 1.0 ;					// o/p potential when spiking
		this.learningWindow = 0.001 ;  			// learn from spikes happening max this far apart (in time) 
		this.refractoryDelay = 0.005;			// delay between spikes ( see refractoryFactor below )
		this.id = id ;
		this.thresholdLearningRate = 0.00005 ;	// how fast to change threshold when not spiking
		
		this.currentPotential = rng.nextDouble() ;
		this.lastSpikeTime = -1.0 ;
		this.refractoryFactor = 1.0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;
	}

	public Neuron( Genome genome, int id ) {
		this.id = id ;
		
		this.threshold = genome.getDouble( GENOME_INDEX_THRESHOLD ) + 0.5 ;
		this.restingPotential = genome.getDouble( GENOME_INDEX_RESTING ) ;
		this.decay = genome.getDouble( GENOME_INDEX_DECAY ) ;
		this.learningRate = genome.getDouble( GENOME_INDEX_LEARNING_RATE ) ;
		this.spikeValue = genome.getDouble( GENOME_INDEX_SPIKE_VALUE ) + 0.5 ;
		this.learningWindow = genome.getDouble( GENOME_INDEX_LEARNING_WINDOW ) ;
		this.refractoryDelay = genome.getDouble( GENOME_INDEX_REFRACTORY_DELAY ) ;
		this.thresholdLearningRate = genome.getDouble( GENOME_INDEX_THRESHOLD_LEARNING_RATE ) ;

		this.currentPotential = rng.nextDouble() ;
		this.lastSpikeTime = -1.0 ;
		this.refractoryFactor = 1.0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( threshold - 0.5, GENOME_INDEX_THRESHOLD ) ;
		rc.set( restingPotential, GENOME_INDEX_RESTING ) ;
		rc.set( decay, GENOME_INDEX_DECAY ) ;
		rc.set( learningRate, GENOME_INDEX_LEARNING_RATE ) ;
		rc.set( spikeValue - 0.5, GENOME_INDEX_SPIKE_VALUE ) ;
		rc.set( learningWindow, GENOME_INDEX_LEARNING_WINDOW ) ;
		rc.set( refractoryDelay, GENOME_INDEX_REFRACTORY_DELAY ) ;
		rc.set( thresholdLearningRate, GENOME_INDEX_THRESHOLD_LEARNING_RATE ) ;

		return rc ;
	}


	public void decay() {
		this.currentPotential *= ( 1.0 - decay ) ;
	}

	public void step( double potential, double clock ) {
		updateRefractoryFactor( clock ) ;
		if( isSpiking() ) {
			isSpiking = false ;
			currentPotential = restingPotential ;
			resetRefractoryFactor(clock);
		}
		decay() ;
		this.currentPotential += potential * refractoryFactor ;
		
		if( this.currentPotential < restingPotential ) {
			this.currentPotential = restingPotential ;
		} 			
	}

	
	public void checkForSpike( double clock ) {
		if( this.currentPotential>threshold ) {
			spike( clock ) ;
		} 			
	}	

	
	public void spike( double clock ) {
		isSpiking = true ;
		lastSpikeTime = clock ;		

		lastSpikes[ lastSpikeIndex ] = clock ;
		lastSpikeIndex++ ;
		if( lastSpikeIndex >= lastSpikes.length ) {
			lastSpikeIndex = 0 ;
		}			
	}

	public void train( Brain brain, double clock ) {
		// Look for pre-synaptic spikes (we received a spike before we spiked)
		// and post-synaptic spikes (we spiked before receiving a spike)
		EdgeList edges = brain.getIncomingEdges( id ) ;
		for( Edge e : edges ) {		
			Neuron source = brain.getNeuron( e.source() ) ;
			double tsrc = source.timeSinceFired( clock ) ;
			double ttgt = timeSinceFired( clock ) ;
			// TODO consider simultaneous 
			boolean srcFiredRecently = tsrc < learningWindow ;
			boolean tgtFiredRecently = ttgt < learningWindow ;
			boolean srcFiredFirst = tsrc > ttgt ;

			// if we are spiking ...
			// 	enhance weights fired recently
			// 	else suppress weight
			// else (not spiking)
			//	suppress weights fired recently
			//	else nothing

			if( srcFiredRecently && tgtFiredRecently && srcFiredFirst ) {
				// reinforce
				e.scaleWeight( 1.0 + 30 * learningRate ) ;
			} else if( srcFiredRecently && tgtFiredRecently && !srcFiredFirst ) {
				// suppress
				e.scaleWeight( 1 - 1 * learningRate ) ;
			} else if( tgtFiredRecently && !srcFiredRecently ) {
				// suppress
				e.scaleWeight( 1 - 1 * learningRate ) ;
			} else if( !tgtFiredRecently && srcFiredRecently ) {
				// suppress
				//e.scaleWeight( 1 - learningRate ) ;
			} else {
				// nothing if neither fired recently
			}
		}
	}
	
	public void updateFrequency( double clock ) {

		// remove any very old spikes from history
		// they won't count towards frequency calc.
		for( int i=0 ; i<lastSpikes.length ; i++ ) {
			if( (clock - lastSpikes[i]) > .2 ) {
				lastSpikes[i] = 0 ;
			}
		}

		int numSpikes = 0 ;
		// find earliest spike
		double earliestSpike = Double.MAX_VALUE ; 
		for( int i=0 ; i<lastSpikes.length ; i++ ) {
			if( lastSpikes[i] > 0 ) {
				numSpikes++ ;
				earliestSpike = Math.min( earliestSpike, lastSpikes[i] ) ;
			}
		}
		
		// freq = spikes / second
		double dt = clock - earliestSpike ;
		if( dt < 1e-9 ) dt = 1e-9 ;  // zero would be bad ( i.e 0/0 ) 
		frequency = numSpikes / dt ;
	}
	
	
	public double frequency() {		
		return frequency ;
	}
	

	public double calculateRefractoryFactor( double clock ) {
		double refractoryFactor = ( clock - refractoryPeriodStart ) / refractoryDelay ;
		refractoryFactor *= refractoryFactor ;
		if( refractoryFactor > 1.0 ) {
			refractoryFactor = 1.0 ;
		}
		return refractoryFactor ;
	}
	
	public void updateRefractoryFactor( double clock ) {
		this.refractoryFactor = calculateRefractoryFactor(clock) ; 
	}
	public void resetRefractoryFactor( double clock ) {
		this.refractoryFactor = 0 ;
		this.refractoryPeriodStart = clock ;
	}
	public double getRefractoryFactor() {
		return this.refractoryFactor ; 
	}
	
	public double timeSinceFired( double clock ) { return clock - lastSpikeTime ; }
	public int getId() { return id ; }
	public double getPotential() { return currentPotential ; }
	public double getRestingPotential() { return restingPotential ; }
	public double getDecay() { return decay ; }
	public double getThreshold() { return threshold ; }
	public double getThresholdLearningRate() { return thresholdLearningRate ; }
	public double getLearningRate() { return learningRate ; }
	public boolean isSpiking() { return isSpiking ; }
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( System.lineSeparator() ) ;
		sb
		.append( "id          ").append( id ).append( System.lineSeparator() )
		.append( "potential   ").append( currentPotential ).append( System.lineSeparator() ) 
		.append( "threshold   ").append( threshold ).append( System.lineSeparator() ) 
		.append( "frequency   ").append( frequency() ).append( System.lineSeparator() ) 
		.append( "refractory  ").append( refractoryDelay ).append( System.lineSeparator() ) 
		.append( "ref. factor ").append( refractoryFactor ).append( System.lineSeparator() ) 
		.append( "spike       ").append( spikeValue ).append( System.lineSeparator() ) 
		.append( "last spike  ").append( lastSpikeTime ).append( System.lineSeparator() ) 
		.append( "spiking     ").append( isSpiking ).append( System.lineSeparator() ) 
		.append( "decay       ").append( decay ).append( System.lineSeparator() ) 
		;
		return sb.toString() ;
	}
}



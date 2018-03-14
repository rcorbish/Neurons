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
	private			boolean		isSuppressed ;
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
		this.restingPotential = 0 ; 
		this.threshold = 0.8 ; 
		this.decay = 0.25 ; 	
		this.learningRate = 0.001 ;
		this.spikeValue = 1.0 ;
		this.learningWindow = 0.02 ;  	// 20mS
		this.refractoryDelay = 0.002;	// delay between spikes ( see refractoryFactor below )
		this.id = id ;
		this.thresholdLearningRate = 0.00005 ;
		
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

	public void step( double potential, double clock ) {
		isSpiking = false ;
				
		if( this.currentPotential>threshold ) {
			spike( clock ) ;
		} else {			
			decay() ;
			this.currentPotential += potential * refractoryFactor ;
			
			if( this.currentPotential < restingPotential ) {
				this.currentPotential = restingPotential ;
			} 			
		}

		for( int i=0 ; i<lastSpikes.length ; i++ ) {
			if( (clock - lastSpikes[i]) > 1.0 ) {
				lastSpikes[i] = 0 ;
			}
		}
	}

	public void suppressSpike( double clock ) {
		isSuppressed = true ;
		isSpiking = false ;
		refractoryPeriodStart = clock ;
	}

	public void decay() {
		this.currentPotential *= ( 1.0 - decay ) ;
	}
	
	public void spike( double clock ) {
		isSpiking = true ;
		lastSpikeTime = clock ;
		refractoryPeriodStart = clock ;
		this.currentPotential -= threshold ;
		
		lastSpikes[ lastSpikeIndex ] = clock ;
		lastSpikeIndex++ ;
		if( lastSpikeIndex >= lastSpikes.length ) {
			lastSpikeIndex = 0 ;
		}			
	}
	
	


	public void train( Brain brain, double clock ) {
		if( lastSpikeTime < 0 ) {
			return ;
		}
//		if( isSpiking() ) {
			// Look for pre-synaptic spikes (we received a spike before we spiked)
			// and post-synaptic spikes (we spiked before receiving a spike)
			EdgeList edges = brain.getIncomingEdges( id ) ;
			for( Edge e : edges ) {		
				Neuron source = brain.getNeuron( e.source() ) ;
				double tpre = source.timeSinceFired( clock ) ;
				double tpost = timeSinceFired( clock )  ;
				double deltaFiredTime = tpost - tpre ;
				// pre-synaptic spike occurs before 
				if( deltaFiredTime > 0 && deltaFiredTime < learningWindow ) {
					//reinforce
					double delta = learningRate * ( 0.1 + e.weight() * ( 0.9 - e.weight() ) ) ;
					e.addWeight( delta ) ;
				}
			}
//		}
		
		// Look for pre-synaptic spikes (we received a spike before we spiked)
		// and post-synaptic spikes (we spiked before receiving a spike)
		EdgeList edges2 = brain.getOutgoingEdges( id ) ;
		for( Edge e : edges2 ) {		
			Neuron target = brain.getNeuron( e.target() ) ;
//			if( target.isSpiking() ) {
				double tpost = target.timeSinceFired( clock ) ;
				double tpre = timeSinceFired( clock )  ;
				double deltaFiredTime = tpost - tpre ;
				// post-synaptic spike occurs before pre-synaptic 
				if( deltaFiredTime <= 0 && deltaFiredTime > -learningWindow ) {
					//suppress
					double delta = 0.9 * learningRate * ( 0.1 + e.weight() * ( 0.9 - e.weight() ) ) ;
					e.addWeight( -delta  ) ;
				}
//			}
		}
		
		if( isSpiking() ) {
			threshold += threshold * thresholdLearningRate ;
			if( threshold > 1 ) threshold = 1 ;
		} else {
			threshold -= threshold * thresholdLearningRate / 100 ;
			if( threshold < 0.2 ) threshold = 0.2 ;
		}
	}
	
	public void updateFrequency( double clock ) {
		double earliestSpike = lastSpikes[0] ; 

		for( int i=1 ; i<lastSpikes.length ; i++ ) {
			if( lastSpikes[i] > 0 ) {
				earliestSpike = Math.min( earliestSpike, lastSpikes[i] ) ;
			}
		}

		// If we haven't filled up the buffer ... 
		if( earliestSpike == 0 ) {
			frequency = 0 ;
		} else {
			double dt = clock - earliestSpike ;
			frequency = ( dt < 1e-6 ) ? 10_000 : (lastSpikes.length / dt) ;
		}
	}
	
	public double frequency() {		
		return frequency ;
	}
	

	public double calculateRefractoryFactor( double clock ) {
		double refractoryFactor = ( clock - refractoryPeriodStart ) / refractoryDelay ;
		refractoryFactor *= refractoryFactor ;
		if( refractoryFactor > 1.0 ) {
			isSuppressed = false ;
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
		.append( "suppressed  ").append( isSuppressed ).append( System.lineSeparator() ) 
		.append( "decay       ").append( decay ).append( System.lineSeparator() ) 
		;
		return sb.toString() ;
	}
}



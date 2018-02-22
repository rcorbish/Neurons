package com.rc ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Neuron  {

	final static Logger log = LoggerFactory.getLogger( Neuron.class ) ;

	private final static Random rng = new Random()  ;
	private final static int GENOME_INDEX_THRESHOLD = 0 ;
	private final static int GENOME_INDEX_RESTING = 1 ;
	private final static int GENOME_INDEX_DECAY = 2 ;
	private final static int GENOME_INDEX_ID = 3 ;
	private final static int GENOME_INDEX_LEARNING_RATE = 4 ;
	private final static int GENOME_INDEX_SPIKE_VALUE = 5 ;
	private final static int GENOME_INDEX_LEARNING_WINDOW = 6 ;
	private final static int GENOME_INDEX_REFRACTORY_DELAY = 7 ;
	public  final static int GENOME_SIZE = 8 ;

	// These are transient state data
	private 		double 		currentPotential ;
	private 		boolean		isSpiking ;
	private  		double 		lastSpikeTime ;		// when did we spike last
	private 		double		refractoryEnd ;
	
	// The following items are held in the genome
	private final int 		id ;
	private final double 	learningRate ;
	private final double 	threshold  ;
	private final double 	decay ;
	private final double 	spikeValue ;
	private final double 	refractoryDelay ;	
	private final double 	restingPotential ;
	private final double 	learningWindow ;

	public Neuron( int id) {
		this.restingPotential = 0 ; 
		this.threshold = 0.8 ; 
		this.decay = 0.01 ; 	
		this.learningRate = 0.001 ;
		this.spikeValue = 1.0  ;
		this.learningWindow = 0.02 ;  	// 20mS
		this.refractoryDelay = 0.001;	// 1mS
		this.id = id ;

		this.currentPotential = rng.nextDouble() ;
		lastSpikeTime = -1.0 ;
	}

	public Neuron( Genome genome ) {
		this.threshold = genome.getDouble( GENOME_INDEX_THRESHOLD ) ;
		this.restingPotential = genome.getDouble( GENOME_INDEX_RESTING ) ;
		this.decay = genome.getDouble( GENOME_INDEX_DECAY ) ;
		this.id = genome.getInt( GENOME_INDEX_ID ) ;
		this.learningRate = genome.getDouble( GENOME_INDEX_LEARNING_RATE ) ;
		this.spikeValue = genome.getDouble( GENOME_INDEX_SPIKE_VALUE ) ;
		this.learningWindow = genome.getDouble( GENOME_INDEX_LEARNING_WINDOW ) ;
		this.refractoryDelay = genome.getDouble( GENOME_INDEX_REFRACTORY_DELAY ) ;

		this.currentPotential = rng.nextDouble() ;
		lastSpikeTime = -1.0 ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( id, GENOME_INDEX_ID ) ;
		rc.set( threshold, GENOME_INDEX_THRESHOLD ) ;
		rc.set( restingPotential, GENOME_INDEX_RESTING ) ;
		rc.set( decay, GENOME_INDEX_DECAY ) ;
		rc.set( learningRate, GENOME_INDEX_LEARNING_RATE ) ;
		rc.set( spikeValue, GENOME_INDEX_SPIKE_VALUE ) ;
		rc.set( learningWindow, GENOME_INDEX_LEARNING_WINDOW ) ;
		rc.set( refractoryDelay, GENOME_INDEX_REFRACTORY_DELAY ) ;

		return rc ;
	}

	public void setPotential( double potential, double clock ) {
		isSpiking = false ;
		
		if( clock < refractoryEnd ) {
			this.currentPotential = restingPotential ;
		} else {			
			decay() ;
			this.currentPotential += potential;
			
			if( this.currentPotential < restingPotential ) {
				this.currentPotential = restingPotential ;
			} 
			
			if( this.currentPotential>threshold ) {
				spike( clock ) ;
			}			
		}
	}
	
	public void decay() {
		this.currentPotential *= ( 1.0 - decay ) ;
	}
	
	public void spike( double clock ) {
		isSpiking = true ;
		lastSpikeTime = clock ;
		this.currentPotential = spikeValue ;
		refractoryEnd = clock + refractoryDelay ;
	}
	public void rest( double clock ) {
		isSpiking = false ;
		this.currentPotential = restingPotential ;
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
				Neuron source = brain.findNeuron( e.source() ) ;
				double tpre = source.timeSinceFired( clock ) ;
				double tpost = timeSinceFired( clock )  ;
				double deltaFiredTime = tpost - tpre ;
				// pre-synaptic spike occurs before 
				if( deltaFiredTime > 0 && deltaFiredTime < learningWindow ) {
					//reinforce
					double delta = learningRate * e.weight() * ( 1 - e.weight() ) ;
					e.addWeight( delta ) ;
				}
			}
//		}
		
		// Look for pre-synaptic spikes (we received a spike before we spiked)
		// and post-synaptic spikes (we spiked before receiving a spike)
		EdgeList edges2 = brain.getOutgoingEdges( id ) ;
		for( Edge e : edges2 ) {		
			Neuron target = brain.findNeuron( e.target() ) ;
//			if( target.isSpiking() ) {
				double tpost = target.timeSinceFired( clock ) ;
				double tpre = timeSinceFired( clock )  ;
				double deltaFiredTime = tpost - tpre ;
				// post-synaptic spike occurs before pre-synaptic 
				if( deltaFiredTime <= 0 && deltaFiredTime > -learningWindow ) {
					//suppress
					double delta = 0.9 * learningRate * e.weight() * ( 1 - e.weight() ) ;
					e.addWeight( -delta  ) ;
				}
//			}
		}
	}
	
	
	public double timeSinceFired( double clock ) { return clock - lastSpikeTime ; }
	public int getId() { return id ; }
	public double getPotential() { return currentPotential ; }
	public double getRestingPotential() { return restingPotential ; }
	public double getDecay() { return decay ; }
	public double getThreshold() { return threshold ; }
	public boolean isSpiking() { return isSpiking ; }
}



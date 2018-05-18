package com.rc.neurons ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Brain;
import com.rc.Edge;
import com.rc.EdgeList;
import com.rc.Genome;


abstract public class Neuron  {

	final static Logger log = LoggerFactory.getLogger( Neuron.class ) ;

	protected final static int NUM_SPIKES_TO_RECORD = 25 ;
	
	protected final static Random rng = new Random()  ;
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
	
	protected final boolean isInhibitor ;
	protected final double	a ;
	protected final double	b ;
	protected final double	c ;		// resting potential
	protected final double	d ;		

	protected double	u ;
	private final static double tau = 10 ;
	
	// The following items are held in the genome
	private final int 		id ;
	private final double 	learningRate ;
	protected     double 	threshold  ;
	private 	  double 	lastStepClock ;
	private 	  double	frequency ;
	private final double 	lastSpikes[] ;
	int lastSpikeIndex ;

	protected Neuron( int id, double A, double B, double C, double D, boolean inhibitor  ) {
		this.id = id ;

		this.isInhibitor = inhibitor ;
		this.a = A ;
		this.b = B ;
		this.c = C ;
		this.d = D ;
		
		this.u = 0 ;
		
		this.threshold = .030 ; 				// spike triggered when internal potential hit this value

		this.learningRate = 0.03 ;				// how fast to adjust weights
		
		this.currentPotential = -.07 ; //rng.nextDouble() ;
		this.lastSpikeTime = 0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;
	}

	public Neuron( Genome genome, int id ) {
		this.id = id ;
		
		this.threshold = genome.getDouble( GENOME_INDEX_THRESHOLD ) + 0.5 ;
		this.learningRate = genome.getDouble( GENOME_INDEX_LEARNING_RATE ) ;

		this.currentPotential = rng.nextDouble() ;
		this.lastSpikeTime = -1.0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;

		this.a = 0.02 ;
		this.b = 0.2 ;
		this.c = -65 ;
		this.d = 8 ;
		this.isInhibitor = false ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( threshold - 0.5, GENOME_INDEX_THRESHOLD ) ;
		rc.set( learningRate, GENOME_INDEX_LEARNING_RATE ) ;

		return rc ;
	}


	public void step( double potential, double clock ) {
		if( isSpiking() ) {
			isSpiking = false ;
			lastStepClock = clock ;

			//----------------------------
			// Reset ODE params on a spike
			this.currentPotential = c / 1000.0  ;
			this.u += this.d ;
			
			return ;
		}
		//--------------------------------------------
		// ODE params are in millivolts & milliseconds
		// for now do this ...
		//
		double dt = ( clock - lastStepClock ) * 1000.0 ;
		lastStepClock = clock ;
		
		double cp = currentPotential * 1000.0 ;
		double p  = potential * 1000.0 ;
		
		double v =  0.04 * cp * cp + 
					5 * cp +
					140 - 
					u +
					p
				;
		this.u += dt * this.a * ( this.b * cp - this.u ) ;
		this.currentPotential += dt * v / 1000.0 ;
	}

	
	public void checkForSpike( double clock ) {
		if( this.currentPotential>threshold ) {
			spike( clock ) ;
		} 			
	}	

	
	public void spike( double clock ) {
		isSpiking = true ;
		lastSpikeTime = clock ;		
		currentPotential = threshold ;
		lastSpikes[ lastSpikeIndex ] = clock ;
		lastSpikeIndex++ ;
		if( lastSpikeIndex >= lastSpikes.length ) {
			lastSpikeIndex = 0 ;
		}			
	}

	public void train( Brain brain, double clock ) {
		
		
		// NB this relies on step() being called
		double dt = ( clock - lastStepClock ) * 1000.0 ;
		
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
	
	
	public double timeSinceFired( double clock ) { return clock - lastSpikeTime ; }
	public int getId() { return id ; }
	public double getPotential() { return currentPotential ; }
	public double getRestingPotential() { return c ; }
	public double getThreshold() { return threshold ; }
	public double getLearningRate() { return learningRate ; }
	public boolean isSpiking() { return isSpiking ; }
	public boolean isInhibitor() { return isInhibitor ; }
	
	public String getType() { 
		String className = getClass().getName() ; 
		return className.substring( className.lastIndexOf('.')+1 ) ; 
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( System.lineSeparator() ) ;
		sb
		.append( "type        ").append( getType() ).append( System.lineSeparator() )
		.append( "id          ").append( id ).append( System.lineSeparator() )
		.append( "potential   ").append( currentPotential ).append( System.lineSeparator() ) 
		.append( "threshold   ").append( threshold ).append( System.lineSeparator() ) 
		.append( "frequency   ").append( frequency() ).append( System.lineSeparator() ) 
		.append( "last spike  ").append( lastSpikeTime ).append( System.lineSeparator() ) 
		.append( "spiking     ").append( isSpiking ).append( System.lineSeparator() ) 
		;
		return sb.toString() ;
	}
}



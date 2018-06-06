package com.rc.neurons ;

import java.util.Random;

import org.ejml.data.DMatrixSparseCSC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Brain;


abstract public class Neuron  {

    //-------------------------------------------
    // constants
    private final static int NUM_SPIKES_TO_RECORD = 25 ;

    //-------------------------------------------
    // shared utils
    final static protected Logger log = LoggerFactory.getLogger( Neuron.class ) ;
    final static protected Random rng = new Random() ;

    //-------------------------------------------
    // transient state data
	protected 		double 		currentPotential ;
	protected 		boolean		isSpiking ;

	private final 	double 		lastSpikes[] ;
	private 	  	int 		lastSpikeIndex ;

	protected 		double		u ;

    private 	  	double 		lastStepClock ;
    private 	  	double		frequency ;

    //-------------------------------------------
	// genome static data
	private final int 		id ;

	protected final double	a ;
	protected final double	b ;
	protected final double	c ;		// resting potential
	protected final double	d ;		

	private final double 	threshold  ;
	private final double 	spikeValue  ;
	private final double 	spikeDuration  ;
	
    private final double 	learningRate ;
    private final double 	learningRateTau ;
    private final double 	learningWindowLTP ;     // pre->post = long term potentiation
    private final double 	learningWindowLTD ;     // post->pre = long term depression

    //-------------------------------------------



	protected Neuron( int id, double A, double B, double C, double D ) {
		this.id = id ;

		this.a = A ;
		this.b = B ;
		this.c = C ;
		this.d = D ;
		
		this.u = 0 ;
		
		this.threshold = .030 ; 			// spike triggered when internal potential hit this value
		this.spikeValue = .450 ; 			// height of spike pulse
		this.spikeDuration = 0.0001 ;		// length of spike pulse

		this.learningRate = 0.001 ;			// how fast to adjust weights
        this.learningRateTau = 0.04 ;       // exp decay of learning wrt time between spikes
        this.learningWindowLTP = 0.020 ;    // ms for LTP
        this.learningWindowLTD = 0.025 ;    // mS for LTD

		this.currentPotential = -.07 ;
		this.lastSpikeIndex = 0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;
	}


	public void step( double potential, double clock ) {
		if( isSpiking() ) {
			if( timeSinceFired(clock)>=spikeDuration ) {
				reset() ;
			}
			lastStepClock = clock ;
		} else {
			//--------------------------------------------
			// ODE params are in millivolts & milliseconds
			// for now do this ...
			//
			double dt = (clock - lastStepClock) * 1000.0;
			lastStepClock = clock;

			double cp = currentPotential * 1000.0;
			double p = potential * 1000.0;

			double v = (0.04 * cp + 5) * cp + 140 - u + p ;
			this.u += dt * this.a * ( this.b * cp - this.u ) ;
			this.currentPotential += dt * v / 1000.0;
		}
	}


	public void reset( ) {
		isSpiking = false ;

		//----------------------------
		// Reset ODE params on a spike
		this.currentPotential = c / 1000.0  ;
		this.u += this.d ;
	}


	public boolean checkForSpike( double clock ) {
	    boolean rc = false ;
		if( this.currentPotential>threshold ) {
			spike( clock ) ;
			rc = true ;
		}
		return rc ;
	}	

	
	public void spike( double clock ) {
		lastSpikeIndex++ ;
		if( lastSpikeIndex >= lastSpikes.length ) {
			lastSpikeIndex = 0 ;
		}			
		isSpiking = true ;
		//currentPotential = spikeValue ;
		lastSpikes[ lastSpikeIndex ] = clock ;
	}


	public void train( Brain brain, double clock, DMatrixSparseCSC training ) {
		if( isSpiking() ) {
			Neuron sources[] = brain.getInputsTo( id ) ;
			for( Neuron src : sources ) {
				double srcFiredAgo = src.timeSinceFired(clock);
				double dw = 0 ;
                if ( srcFiredAgo==0 ) {
                    dw = -learningRate / 10.0 ;
                } else if (srcFiredAgo < learningWindowLTP ) {
                    dw = learningRate * Math.exp( (learningWindowLTP-srcFiredAgo) / learningRateTau ) ;
				}
				if( dw != 0 ) {
					training.set( id, src.id, training.get( id, src.id ) + dw ) ;
				}
			}

			Neuron targets[] = brain.getOutputsFrom( id ) ;
			for( Neuron tgt : targets ) {
				double tgtFiredAgo = tgt.timeSinceFired(clock);
                if( tgtFiredAgo < learningWindowLTD && tgtFiredAgo>0 ) {
                    double dw = -learningRate * Math.exp( (learningWindowLTD-tgtFiredAgo) / learningRateTau ) ;
					training.set( tgt.id, id, training.get( tgt.id, id ) + dw ) ;
				}
			}
		}
	}
	

	public void updateFrequency( double clock ) {

		// remove any very old spikes from history
		// they won't count towards frequency calc.
		for( int i=0 ; i<lastSpikes.length ; i++ ) {
			if( (clock - lastSpikes[i]) > .02 ) {
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
		if( dt < 1e-9 ) dt = 1e-9 ;  // zero would be bad ( i.e x/0 )
		frequency = numSpikes / dt ;
	}
	
	
	public double frequency() {		
		return frequency ;
	}
	
	
	public double timeSinceFired( double clock ) { 
		double lastSpikeTime = lastSpikes[ lastSpikeIndex ] ;
		return clock - lastSpikeTime ; 
	}

	public int getId() { return id ; }
	public double getPotential() { return currentPotential ; }
	public double getThreshold() { return threshold ; }
	public double getSpikeValue() { return spikeValue ; }
	public double getLearningRate() { return learningRate ; }
	public boolean isSpiking() { return isSpiking ; }
    abstract public boolean isInhibitor() ;
    abstract public NeuronType getType() ;


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( System.lineSeparator() ) ;
		sb
		.append( "type        ").append( getType() ).append( System.lineSeparator() )
		.append( "id          ").append( getId() ).append( System.lineSeparator() )
		.append( "potential   ").append( getPotential() ).append( System.lineSeparator() ) 
		.append( "frequency   ").append( frequency() ).append( System.lineSeparator() ) 
		.append( "spiking     ").append( isSpiking() ).append( System.lineSeparator() ) 
		;
		return sb.toString() ;
	}
}



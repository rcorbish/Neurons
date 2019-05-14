package com.rc.neurons ;

import java.util.Random;

import org.ejml.data.DMatrixSparseCSC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Brain;

/**
 * This is the base code for a Neuron. Each real neuron type will define the ODE
 * params A,B,C & D .
 *
 * @link http://www.izhikevich.org/human_brain_simulation/Blue_Brain.htm
 * @link http://www.izhikevich.org/publications/spikes.htm
 */
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
	private 		double 		currentPotential ;
	private 		boolean		isSpiking ;

	private final 	double 		lastSpikes[] ;
	private 	  	int 		lastSpikeIndex ;

	protected 		double		u ;

    private 	  	double 		lastStepClock ;
    private 	  	double		frequency ;
    private         double 	    spikeValue  ;

    //-------------------------------------------
	// genome static data
	private final int 		id ;

	protected final double	a ;
	protected final double	b ;
	protected final double	c ;		// resting potential
	protected final double	d ;		

	private final double 	threshold  ;

    private final double 	learningRate ;
    private final double 	learningRateTauLTP ;
    private final double 	learningRateTauLTD ;
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
		
		this.threshold = -30 ;   			// spike triggered when internal potential hit this value
		this.spikeValue = 30 ; 	    		// height of spike pulse

		this.learningRate = 0.03 ;		    // how fast to adjust weights
        this.learningRateTauLTP = 2 ;       // exp decay of learning wrt time between spikes
        this.learningRateTauLTD = 4 ;       // ... for depression
        this.learningWindowLTP = 20 ;       // ms for LTP window
        this.learningWindowLTD = 50 ;       // mS for LTD window

		this.currentPotential = -50 ;
		this.lastSpikeIndex = 0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;
	}


	public void step( double potential, double clock ) {
		if( isSpiking() ) {
			reset() ;
		} else {
			// convert potential in mV to current 
			double i = potential * 3.0 ;
			//--------------------------------------------
			// ODE params are in millivolts & milliseconds
			//
			double dt = (clock - lastStepClock)  ;

			double cp = currentPotential ;

			double v = (0.04 * cp + 5) * cp + 140 - u + i ;
			this.u += dt * this.a * ( this.b * cp - this.u ) ;
			this.currentPotential += dt * v ;

			checkForSpike( clock ) ;
		}
		lastStepClock = clock ;
	}


	public void reset( ) {
		isSpiking = false ;

		//----------------------------
		// Reset ODE params on a spike
		this.currentPotential = c  ;
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
		lastSpikes[ lastSpikeIndex ] = clock ;
	}


	public void train( Brain brain, DMatrixSparseCSC training ) {
		if( isSpiking() ) {
			double lst = lastSpikeTime() ;
			Neuron sources[] = brain.getInputsTo( id ) ;
			for( Neuron src : sources ) {
                double dt = lst - src.lastSpikeTime() ;
                if( dt < learningWindowLTD && dt <= 0 ) {
                    double dw = learningRate * Math.exp( dt / learningRateTauLTD ) ;
                    brain.addTraining( src.id, id, dw ) ;
                } else if ( -dt < learningWindowLTP && dt > 0  ) {
                    double dw = learningRate * Math.exp( -dt / learningRateTauLTP ) ;
                    brain.addTraining( src.id, id, dw ) ;
                }
			}
		}
	}
	

	public void updateFrequency( double clock ) {

		// remove any very old spikes from history
		// they won't count towards frequency calc.
		for( int i=0 ; i<lastSpikes.length ; i++ ) {
			if( (clock - lastSpikes[i]) > 200 ) {
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
		frequency = numSpikes / (dt/1000.0) ;
	}
	
	
	public double frequency() {		
		return frequency ;
	}
	
	
	public double timeSinceFired( double clock ) { 
		double lastSpikeTime = lastSpikes[ lastSpikeIndex ] ;
		return clock - lastSpikeTime ; 
	}

	public double lastSpikeTime() { 
		return  lastSpikes[ lastSpikeIndex ] ;
	}

	public int getId() { return id ; }
	public double getPotential() { return currentPotential ; }
	public double getThreshold() { return threshold ; }
	public double getSpikeValue() { return spikeValue ; }
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



package com.rc.neurons ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Brain;
import com.rc.Genome;


abstract public class Neuron  {

    //-------------------------------------------
    // constants
    private final static int NUM_SPIKES_TO_RECORD = 25 ;

	private final static int GENOME_INDEX_TYPE = 0 ;
	private final static int GENOME_INDEX_A = 1 ;
	private final static int GENOME_INDEX_B = 2 ;
	private final static int GENOME_INDEX_C = 3 ;
	private final static int GENOME_INDEX_D = 4 ;
	private final static int GENOME_INDEX_LEARNING_RATE = 5 ;
	public  final static int GENOME_SIZE = 6 ;

    //-------------------------------------------
    // shared utils
    final static protected Logger log = LoggerFactory.getLogger( Neuron.class ) ;
    final static protected Random rng = new Random() ;

    //-------------------------------------------
    // transient state data
	protected 		double 		currentPotential ;
	protected 		boolean		isSpiking ;

	private final double 	lastSpikes[] ;
	private 	  int 		lastSpikeIndex ;

	protected double	u ;

    private       double 	threshold  ;
    private 	  double 	lastStepClock ;
    private 	  double	frequency ;

    //-------------------------------------------
	// genome static data
	private final int 		id ;

	protected final double	a ;
	protected final double	b ;
	protected final double	c ;		// resting potential
	protected final double	d ;		

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

		this.learningRate = 0.1 ;			// how fast to adjust weights
        this.learningRateTau = 0.02 ;       // exp decay of learning wrt time between spikes
        this.learningWindowLTP = 0.015 ;    // 0 .. 15ms  for LTP
        this.learningWindowLTD = 0.100 ;    // 15 .. 100 for LTD

		this.currentPotential = -.07 ; 		//rng.nextDouble() ;
		this.lastSpikeIndex = 0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;
	}


	public Neuron( Genome genome, int id ) {
		this.id = id ;
		
		this.learningRate = genome.getDouble( GENOME_INDEX_LEARNING_RATE ) ;
		this.a = genome.getDouble( GENOME_INDEX_A ) ;
		this.b = genome.getDouble( GENOME_INDEX_B ) ;
		this.c = genome.getDouble( GENOME_INDEX_C ) ;
		this.d = genome.getDouble( GENOME_INDEX_D ) ;

        this.learningRateTau = 0.02 ;
        this.learningWindowLTP = 0.015 ;    // 0 .. 15ms  for LTP
        this.learningWindowLTD = 0.100 ;    // 15 .. 100 for LTD

		this.currentPotential = -0.070 ;
		this.lastSpikeIndex = 0 ;
		this.lastSpikes = new double[NUM_SPIKES_TO_RECORD] ;
	}


	public Genome toGenome() {
		Genome rc = new Genome() ;

		rc.set( learningRate, GENOME_INDEX_LEARNING_RATE ) ;
		rc.set( a, GENOME_INDEX_A ) ;
		rc.set( b, GENOME_INDEX_B ) ;
		rc.set( c, GENOME_INDEX_C ) ;
		rc.set( d, GENOME_INDEX_D ) ;

		return rc ;
	}


	public void step( double potential, double clock ) {
		if( isSpiking() ) {
			reset() ;
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

			double v = (0.04 * cp + 5) * cp + 140 - u + p;
			this.u += dt * this.a * (this.b * cp - this.u);
			this.currentPotential += dt * v / 1000.0;
		}
	}


	public void reset( ) {
//		if( isSpiking() ) {
			isSpiking = false ;

			//----------------------------
			// Reset ODE params on a spike
			this.currentPotential = c / 1000.0  ;
			this.u += this.d ;
//		}
	}


	public void checkForSpike( double clock ) {
		if( this.currentPotential>threshold ) {
			spike( clock ) ;
		} 			
	}	

	
	public void spike( double clock ) {
		lastSpikeIndex++ ;
		if( lastSpikeIndex >= lastSpikes.length ) {
			lastSpikeIndex = 0 ;
		}			
		isSpiking = true ;
		currentPotential = threshold ;
		lastSpikes[ lastSpikeIndex ] = clock ;
	}

	public void train( Brain brain, double clock ) {

		Neuron sources[] = brain.getInputsTo( id ) ;

		if( isSpiking() ) {
			for( Neuron src : sources ) {
			    if( !src.isInhibitor() ) {
                    double srcFiredAgo = src.timeSinceFired(clock);
                    if ( srcFiredAgo==0 ) {
                        brain.addWeight(src.id, id, -learningRate );
                    } else if (srcFiredAgo < learningWindowLTP ) {
                        double dw = learningRate * Math.exp( (learningWindowLTP-srcFiredAgo) / learningRateTau ) ;
                        brain.addWeight(src.id, id, dw );
                    } else if (srcFiredAgo < learningWindowLTD) {
                        double dw = learningRate * Math.exp( (learningWindowLTP-srcFiredAgo) / learningRateTau ) ;
                        brain.addWeight(src.id, id, -dw );
                    }
                } else {
					double srcFiredAgo = src.timeSinceFired(clock);
                    if ( srcFiredAgo==0 ) {
                        brain.addWeight(src.id, id, -learningRate );
                    } else if (srcFiredAgo < learningWindowLTP ) {
                        double dw = learningRate * Math.exp( (learningWindowLTP-srcFiredAgo) / learningRateTau ) ;
                        brain.addWeight(src.id, id, dw );
                    } else if (srcFiredAgo < learningWindowLTD) {
                        double dw = learningRate * Math.exp( (learningWindowLTP-srcFiredAgo) / learningRateTau ) ;
                        brain.addWeight(src.id, id, -dw );
                    }
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
		if( dt < 1e-9 ) dt = 1e-9 ;  // zero would be bad ( i.e 0/0 ) 
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
	public double getRestingPotential() { return c ; }
	public double getThreshold() { return threshold ; }
	public double getLearningRate() { return learningRate ; }
	public boolean isSpiking() { return isSpiking ; }
    abstract public boolean isInhibitor() ;
    abstract public NeuronType getType() ;


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( System.lineSeparator() ) ;
		sb
		.append( "type        ").append( getType() ).append( System.lineSeparator() )
		.append( "id          ").append( id ).append( System.lineSeparator() )
		.append( "potential   ").append( currentPotential ).append( System.lineSeparator() ) 
		.append( "frequency   ").append( frequency() ).append( System.lineSeparator() ) 
		.append( "spiking     ").append( isSpiking() ).append( System.lineSeparator() ) 
		;
		return sb.toString() ;
	}
}



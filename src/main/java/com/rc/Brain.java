package com.rc ;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.neurons.InputNeuron;
import com.rc.neurons.Neuron;
import com.rc.neurons.NeuronFactory;
import com.rc.neurons.NeuronRS;

public class Brain  {

	final static Logger log = LoggerFactory.getLogger( Brain.class ) ;

	private static final Random rng = new Random(24) ;	// utility random number generator

	final static public int HISTORY_LENGTH = 1024 ;
	final static public double CONNECTION_PROBABILITY = 0.50 ;
	
	// Used to calc rands with the following stats
	final static double WEIGHT_MEAN = 0.07 ;
	final static double WEIGHT_SIGMA = 0.15 ;
	
	private final double outputHistory[] ;
	private final boolean outputSpikeHistory[] ;
	private int historyIndex ;
	private int followingId ;
	
	private final float synapses[] ;		// adjacency matrix

	private final int numColumns ;
	private final int numRows ;
	private final Neuron neurons[] ;			// neurons in each layer
	private final Neuron inputNeurons[] ;			// neurons in each layer
	private final Neuron outputNeurons[] ;			// neurons in each layer

	private double clock ;
	private final double tickPeriod ;
	
	private boolean train ;
	private boolean fftSpike ;
	private DoubleFFT_1D fft ;
	
	/**
	 * Create a brain from a genome (bitmask).
	 * 
	 * @param tickPeriod the period each clock tick represents
	 * @param g
	 */
	/*
	public Brain( double tickPeriod, Genome g ) {
		this.tickPeriod = tickPeriod ;
		this.clock = 0 ;
		int numNeurons = 0 ;
		int numlayers  = g.getInt(0) ;
		
		this.neurons = new Neuron[numNeurons] ;

		//-----------------------------
		// how many items in the genome
		//
		// 0 = # layers
		// 1 .. n = # neurons in each layer
		int start = 1 + numlayers ;

		// 1st layer is always special (input) neurons
		for( int i=0 ; i<layerSizes[0] ; i++ ) {
			Genome gs = g.subSequence(start, Neuron.GENOME_SIZE ) ;
			neurons[i] = new InputNeuron( gs, i ) ;
			start += Neuron.GENOME_SIZE ;	// update Brain size
		}
		// Start from 2nd layer onwards ...
		for( int i=layerSizes[0] ; i<neurons.length ; i++ ) {
			Genome gs = g.subSequence(start, Neuron.GENOME_SIZE ) ;
			neurons[i] = new NeuronRS( gs, i  ) ;
			start += Neuron.GENOME_SIZE ;	// update Brain size
		}

		this.numNeurons = numNeurons ;
		targetEdges = new EdgeList[ numNeurons ] ;

		for( int i=0 ; i<targetEdges.length ; i++ ) {
			int numEdges = g.getInt( start ) ;
			Genome gs = g.subSequence(start, numEdges*Edge.GENOME_SIZE + 1 ) ;
			targetEdges[i] = new EdgeList( gs ) ;
			start += numEdges*Edge.GENOME_SIZE + 1 ;
		}
		this.outputHistory = new double[HISTORY_LENGTH] ;
		this.outputSpikeHistory = new boolean[HISTORY_LENGTH] ;
		this.train = false ;
		this.fft = null ;
		this.fftSpike = false ;
	}
*/
	/**
	 * save the brain as a coded sequence. This is used
	 * during evolution.
	 * 
	 */
	/*
	public Genome toGenome() {
		Genome rc = new Genome() ; 
		rc.set( layerSizes.length, 0 ) ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			rc.set( layerSizes[i], 1+i ) ;
		}

		for( int i=0 ; i<neurons.length ; i++ ) {
			rc.append( neurons[i].toGenome() ) ;
		}
		
		for( int i=0 ; i<targetEdges.length ; i++ ) {
			rc.append( targetEdges[i].toGenome() ) ;
		}
		return rc ;
	}
*/

	/**
	 * Create a new instance of a brain.
	 * @param tickPeriod the period of a clock tick
	 * @param layers the number of neurons in each layer
	 */
	public Brain( double tickPeriod, int numInputs, int numOutputs, int rows, int cols ) {

		this.tickPeriod = tickPeriod ;
		this.clock = 0 ;
		this.outputHistory = new double[HISTORY_LENGTH] ; 
		this.outputSpikeHistory = new boolean[HISTORY_LENGTH] ;
		this.historyIndex = 0 ;

		this.numColumns = cols ;
		this.numRows = rows ;

		this.neurons = new Neuron[rows*cols] ;
		this.inputNeurons = new Neuron[numInputs] ;
		this.outputNeurons = new Neuron[numOutputs] ;
		
		int ix = 0 ;
		for( int i=0 ; i<this.neurons.length ; i++, ix++ ) {
			this.neurons[i] = NeuronFactory.getNeuron( ix ) ; // new NeuronRS( i ) ;
		}
		for( int i=0 ; i<this.inputNeurons.length ; i++, ix++ ) {
			this.inputNeurons[i] = new InputNeuron( ix ) ;
		}
		for( int i=0 ; i<this.outputNeurons.length ; i++, ix++ ) {
			this.outputNeurons[i] = new NeuronRS( ix ) ;
		}
		
		this.synapses = new float[ neurons.length*neurons.length ] ;
		connectLayers( CONNECTION_PROBABILITY ) ;
		this.train = false ;
		this.fftSpike = false ;
		this.fft = null ;
	}


	/**
	 * Connects neurons in each layer, to the previous layer
	 * 
	 * @param connectionProbability chance of connecting a pair of neurons ( 1 is fully connected )
	 */
	private void connectLayers( double connectionProbability ) {

		int r = 0 ;
		int c = 0 ;
		for( int i=0 ; i<synapses.length ; i++ ) {

			synapses[i] = 0.0f ;
			if( r!= c ) {
				double p = 0.1 - 0.1 / ( (r-c) * (r-c) ) ;
				if( rng.nextFloat() < p ) {
					synapses[i] = getRandomWeight() ;				
				}
			}
			r++ ;
			if( r>=neurons.length ) {
				c++ ;
				r = 0 ;
			}
		}
	}

	/**
	 * Choose a random weight. Weights may be positive or negative, 
	 * depending on the inhibitor ratio (fraction of negative weights)
	 *  
	 * @return
	 */
	protected float getRandomWeight() {
		double rc = rng.nextGaussian() * WEIGHT_SIGMA + WEIGHT_MEAN ;
		rc = Math.max( rc, 0.1 ) ;
		rc = Math.min( rc, 1.0 ) ;
		return (float)rc ;
	}

	/**
	 * Execute one time step: traverse the graph, 
	 * 	sum all inputs to each neuron. 
	 * 	atomically update the weights
	 * 	find winner in each layer
	 * 	suppress non winning spikes
	 * 	find new spiking neurons
	 * 
	 * @param inputs an array of values to set inputs to
	 */
	public void step( double[] inputs ) {
		clock += tickPeriod ;
 
		// log.info( "Stepping, input len={}, inputNeurons.length={}", inputs.length, inputNeurons.length ) ;
		// Set inputs immediately - no dependencies
		for( int i=0 ; i<inputNeurons.length ; i++ ) {
			this.inputNeurons[i].step( inputs[i], clock ) ;
		}

		// Will build up all outputs - before changing any of them
		double newPotentials[] = calculateNewPotentials() ;

		// Then write the output as an atomic op
		// do NOT write inputs (start from layer 1)
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].step( newPotentials[i], clock ) ;
		}
		
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].checkForSpike(clock) ;
		}		
	}

	
	
	protected double[] calculateNewPotentials() {

		float weights[] = new float[synapses.length] ;

		for( int i=0 ; i<weights.length ; i++ ) {
			int r = i / neurons.length ;
			int c = i % neurons.length ;
			weights[i] = neurons[r].isSpiking() ? synapses[i] : 0f ;
		}

		// 
		// outs = W x c
		//
		// W = n x n
		// c = n x 1
		//
		// @TODO
		// LAPACK sparse - gemm 
		//
		
		// Will build up all outputs - before changing any of them
		double rc[] = new double[ neurons.length ] ;
		for( int i=0 ; i<rc.length ; i++ ) {
			rc[i] = 0 ;
			int j = i % inputNeurons.length ; 
			rc[i] += inputNeurons[j].isSpiking() ? 0.35 : 0 ;
		}

		int ix=0;
		for( int i=0 ; i<neurons.length ; i++ ) { // weight columns
			ix = i*neurons.length ;
			for( int j=0 ; j<neurons.length ; j++ ) {
				rc[j] += weights[ix] * 0.35 ;
				ix++ ;
			}		
		}
		for( int i=0 ; i<rc.length ; i++ ) {
			if( neurons[i].isInhibitor() && neurons[i].isSpiking() ) {
				rc[i] -= 0.085 ;
			}
		}
		return rc ;		
	}

	
	/**
	 * Train all neurons
	 */
	public void train() {
		if( isTrain() ) {
			for( int i=0 ; i<neurons.length; i++ ) {
				neurons[i].train( this, clock ) ;
			}
		}
	}

	/**
	 * If we are following - maintain a history of ouput
	 * and spiking activity
	 */
	public void follow() {
		Neuron following = getNeuron( followingId ) ;
		if( following != null ) {
			outputHistory[historyIndex] = following.getPotential() ;
			outputSpikeHistory[historyIndex] = following.isSpiking() ;
		}
		
		historyIndex++ ;
		if( historyIndex >= outputHistory.length ) {
			historyIndex = 0 ;
		}
		
		for( int i=0 ; i<neurons.length ; i++ ) {
			neurons[i].updateFrequency( clock ) ;
		}
	}


	/**
	 * Get the overall score 
	 * 
	 * @param y the expected pattern index 
	 */
	public double getScore( int y ) {
		return 0 ;
	}



	public Potentials getNeuronPotentials( double clock ) {
		Potentials rc = new Potentials() ;
		rc.clock = clock ;
		rc.history = new double[outputHistory.length] ;
		rc.spikeHistory = new boolean[outputHistory.length] ;

		int offset = historyIndex ;
		
		if( isFourier() ) {
			double tmp[] = new double[ outputHistory.length ] ;
			if( this.fftSpike ) {
				for( int i=0 ; i<tmp.length ; i++ ) {
					tmp[i] = outputSpikeHistory[i] ? 3 : 0 ;
				}
			} else {	
				System.arraycopy( outputHistory, 0, tmp, 0, tmp.length ) ;
			}
			
			fft.realForward( tmp ) ;
			for( int i=0 ; i<tmp.length ; i++ ) {
				offset-- ;
				if( offset<0 ) {
					offset += tmp.length ;
				}
				int ix = outputHistory.length-offset-1 ;
				rc.history[i] = ( tmp[i] + 20 ) / 40.0 ;
				rc.spikeHistory[ix] = outputSpikeHistory[i] ;
			}			
		} else {
			for( int i=0 ; i<outputHistory.length ; i++ ) {
				offset-- ;
				if( offset<0 ) {
					offset += outputHistory.length ;
				}
				int ix = outputHistory.length-offset-1 ;
				rc.history[ix] = ( outputHistory[i] + .100 ) * 4.0 ;
				rc.spikeHistory[ix] = outputSpikeHistory[i] ;
			}
		}
		
		rc.neurons = new ArrayList<NeuronState>() ;
		for( int i=0 ; i<neurons.length; i++ ) {
			rc.neurons.add( new NeuronState( neurons[i], clock ) ) ;
		}
		
		return rc ;
	}


	public CharSequence toJson() {

		StringBuilder rc  = new StringBuilder() ;
		rc.append( "{ \"nodes\": [") ;
		printNodes( rc ) ;
		rc.append( "] }") ;

		return rc ;
	}

	private void printNodes( StringBuilder rc ) {
		char sep = ' '  ;
		int layerWidth = 800 / 10 ;  // cols ?

		int x = 0 ;
		int y = 0 ;
		for( int i=0 ; i<neurons.length ; i++ ) {
			int layerHeight = (600-60) / numRows ;

			rc.append( sep ) 
				.append( "{ \"id\":"  ) 
				.append( neurons[i].getId() ) 
				.append( ",\"potential\":" ) 
				.append( neurons[i].getPotential() ) 
				.append( ",\"threshold\":" ) 
				.append( neurons[i].getThreshold() ) 
				.append( ",\"fx\":").append( x * layerWidth ) 				
				.append( ",\"fy\":").append( y * layerHeight + 30 /* *30+30 */ ) 				
				.append( " }" ) 
				;
				sep = ',' ;
				y++ ;
				if( y>= numRows ) {
					x++ ;
					y = 0 ;
				}
		}
	}


/*
	public boolean save( String fileName ) {
		boolean rc = false ;
		log.info( "Saving to {}", fileName ) ;

		try ( OutputStream os = new FileOutputStream( fileName ) ;
				ObjectOutputStream oos = new ObjectOutputStream(os) )  {
			Genome g = toGenome() ;			
			oos.writeObject( g ) ;
		} catch( IOException ioe ) {
			rc = false ;
			log.warn( "Failed saving brain to {}", ioe.getMessage() );
		}
		return rc ;
	}


	public static Brain load( double tick, String fileName ) {
		Brain rc = null ;
		log.info( "Loading from {}", fileName ) ;
		try ( InputStream is = new FileInputStream( fileName ) ;
				ObjectInputStream ois = new ObjectInputStream(is) )  {
			Genome g = (Genome)ois.readObject() ;
			rc = new Brain( tick, g ) ;
		} catch( Exception ioe ) {
			rc = null ;
			log.warn( "Failed loading brain from {}", ioe.getMessage() );
		}

		return rc ;
	}		
*/


	public Neuron getNeuron( int id ) {
		return id>=0 && id<neurons.length ? neurons[id] : null ;
	}
	
	public int getNumInputs() { return inputNeurons.length ; } 
	public int getNumOutputs() { return outputNeurons.length ; } 
	
	public void setFollowing(int following) {
		if( this.followingId != following ) {
			Neuron n = getNeuron( following ) ;
			if( n != null ) {
				log.info( "Following: {}", n ) ;
			}
			Arrays.fill( outputHistory, 0 ) ;
			Arrays.fill( outputSpikeHistory, false ) ;

		}
		this.followingId = following ;
	}	
	

	public double clock() {
		return clock ;
	}

	public int numNeurons() {
		return neurons.length ;
	}

	public boolean isTrain() {
		return train;
	}

	public void setTrain(boolean train) {
		this.train = train;
	}

	public boolean isFourier() {
		return fft != null ;
	}

	public void setFourier(boolean fourier) {
		this.fft = fourier ? new DoubleFFT_1D(HISTORY_LENGTH) : null ;			
		this.fftSpike = false ;
	}
	
	public void setFourierSpike(boolean fourier) {
		this.fft = fourier ? new DoubleFFT_1D(HISTORY_LENGTH) : null ;
		this.fftSpike = true ;
	}

	public double getTickPeriod() {
		return tickPeriod;
	}
}


class Potentials {
	public double clock ;
	public List<NeuronState> neurons ;
	public double history[] ;
	public boolean spikeHistory[] ;
}

class NeuronState {
	public double potential ;
	public int id ;
	public double frequency ;
	public NeuronState( Neuron n, double clock ) {
		this.potential = n.getPotential() ;
		this.id = n.getId() ;
		this.frequency = n.frequency() ;
	}
}

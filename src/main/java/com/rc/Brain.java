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
	
	private final EdgeList targetEdges[] ;		// adjacency list ( directed & weighted )

	private final Neuron neurons[] ;			// neurons in each layer
	private final int layerSizes[] ;

	private final int numNeurons ;

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
	public Brain( double tickPeriod, Genome g ) {
		this.tickPeriod = tickPeriod ;
		this.clock = 0 ;
		int numNeurons = 0 ;
		int numlayers  = g.getInt(0) ;
		
		layerSizes = new int[numlayers];
		for( int i=0 ; i<numlayers ; i++ ) {
			layerSizes[i] = g.getInt( i+1 ) ;
			numNeurons += layerSizes[i] ;
		}
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

	/**
	 * save the brain as a coded sequence. This is used
	 * during evolution.
	 * 
	 */
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

	/**
	 * Create a new instance of a brain.
	 * @param tickPeriod the period of a clock tick
	 * @param layers the number of neurons in each layer
	 */
	public Brain( double tickPeriod, int ... layers ) {

		this.tickPeriod = tickPeriod ;
		this.clock = 0 ;
		this.outputHistory = new double[HISTORY_LENGTH] ; 
		this.outputSpikeHistory = new boolean[HISTORY_LENGTH] ;
		this.historyIndex = 0 ;
		this.layerSizes = layers ;

		int numNeurons = 0 ;
		
		for( int i=0 ; i<layers.length ; i++ ) {
			numNeurons += layers[i] ;
		}

		this.neurons = new Neuron[numNeurons] ;
						
		for( int i=0 ; i<layers[0] ; i++ ) {
			this.neurons[i] = new InputNeuron( i ) ;
		}

		for( int i=layers[0] ; i<numNeurons ; i++ ) {
			this.neurons[i] = NeuronFactory.getNeuron(i) ; // new NeuronRS( i ) ;
		}

		this.numNeurons = numNeurons ;
		this.targetEdges = new EdgeList[ numNeurons ] ;

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
		// All neurons have edges 
		for( int i=0 ; i<targetEdges.length ; i++ ) {
			this.targetEdges[i] = new EdgeList() ;
		}
		
		// Now connect each layer to the previous layer
		for( int l=1 ; l<layerSizes.length ; l++ ) {
			int s1 = getIndexOfFirstInLayer(l-1) ;
			int s2 = getIndexOfFirstInLayer(l) ;
			for( int i=0 ; i<layerSizes[l-1] ; i++ ) {

				int srcIndex = s1+i ; 
				
				for( int j=0 ; j<layerSizes[l] ; j++ ) {
					int tgtIndex = s2+j ; 
					if( rng.nextDouble() < connectionProbability ) {
						double weight = getRandomWeight() ;
						Edge e = new Edge( srcIndex, tgtIndex, weight ) ;
						targetEdges[ tgtIndex ].add( e ) ;
					}
				}
			}
		}
	}

	/**
	 * Choose a random weight. Weights may be positive or negative, 
	 * depending on the inhibitor ratio (fraction of negative weights)
	 *  
	 * @return
	 */
	protected double getRandomWeight() {
		double rc = rng.nextGaussian() * WEIGHT_SIGMA + WEIGHT_MEAN ;
		rc = Math.max( rc, 0.1 ) ;
		rc = Math.min( rc, 1.0 ) ;
		return rc ;
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

		// Set inputs immediately - no dependencies
		for( int i=0 ; i<layerSizes[0] ; i++ ) {
			this.neurons[i].step( inputs[i], clock ) ;
		}

		// Will build up all outputs - before changing any of them
		double newPotentials[] = calculateNewPotentials() ;

		// Then write the output as an atomic op
		// do NOT write inputs (start from layer 1)
		for( int i=layerSizes[0] ; i<neurons.length; i++ ) {
			neurons[i].step( newPotentials[i], clock ) ;
		}
		
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].checkForSpike(clock) ;
		}		
	}

	
	
	protected double[] calculateNewPotentials() {
		// Will build up all outputs - before changing any of them
		double rc[] = new double[ neurons.length ] ;

		// Sum inputs of spiking neurons
		for( int i=layerSizes[0] ; i<neurons.length; i++ ) {
			rc[i] = 0 ;
			for( Edge edge : targetEdges[i] ) {
				Neuron src = getNeuron( edge.source() ) ;
				if( src.isSpiking() ) {
					rc[i] += edge.weight() ;
					if( src.isInhibitor() ) {
						rc[i] -= 0.085 ;
					}
				}
			}
		}
		return rc ;		
	}

	
		//---------------------------------------------------------
		// if any neurons spiked - reduce prob of other neurons
		// in the same layer firing too (i.e. set their refractory
		//   factors as if they just fired)
		//
		// This effect does not apply to inputs (layers > 0)
		// 
		protected Neuron[] getWinnersInEachLayer() {
				
			Neuron rc[] = new Neuron[ layerSizes.length ] ;

			for( int l=1 ; l<layerSizes.length ; l++ ) {
				int ix = getIndexOfFirstInLayer(l) ;

				// double minRecent = neurons[ix].timeSinceFired( clock ) ;
				double maxPotential =  0 ;
				Neuron winner = null ;
				for( int i=0 ; i<layerSizes[l] ; i++ ) {

					// Find the neuron with max output in a layer
					double potential = neurons[ix+i].getPotential() ;
					if( potential > maxPotential ) {
						winner = neurons[ix+i] ;
						maxPotential = potential ;
					}
				}
				rc[l] = winner ;				
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
		int outputStart = numNeurons-layerSizes[ layerSizes.length-1 ] ;
		
		double sf = 0.0 ;
		for( int i=outputStart ; i<neurons.length ; i++ ) {
			sf += neurons[i].frequency() ;
		}
		double rc = 0 ;
		if( sf == 0 ) { 
			rc = -1e10 ;
		} else {
			for( int i=outputStart ; i<neurons.length ; i++ ) {
				double yh = neurons[i].frequency() / sf ;

				if( y==i ) {
					rc += yh ;	// ideally yh = 1.0 for pth output
				} else {
					rc -= yh ; 	// ideally yh = 0.0 for other outputs
				}
			}
		}
		return rc ;
	}


	/**
	 * Determine in which layer a neuron lies based on its index
	 */
	public int getLayerFromIndex( int index ) {
		
		int rc = -1 ;
		
		if( index < 0 ) {
			return rc ;		// -ve index == WTF
		}
		for( rc=0 ; rc<layerSizes.length ; rc++ ) {
			index -= layerSizes[rc] ;
			if( index < 0 ) {
				return rc ;
			}
		}
		
		return -2 ;		// output neuron
	}

	/**
	 * Given an index - which neuron is it
	 */
	public int getIndexOfFirstInLayer( int layer ) {
		int rc = 0 ;
		for( int i=0 ; i<layer ; i++ ) {
			rc += layerSizes[i] ;
		}
		return rc ;
	}		


	public Potentials getNeuronPotentials( double clock ) {
		Potentials rc = new Potentials() ;
		rc.clock = clock ;
		rc.history = new double[outputHistory.length] ;
		rc.spikeHistory = new boolean[outputHistory.length] ;
		rc.maxFrequency = 0 ;
		rc.minFrequency = 100000 ;

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
		
		for( int i=getIndexOfFirstInLayer( layerSizes.length-1 ) ; i<neurons.length; i++ ) {
			rc.maxFrequency = Math.max( neurons[i].frequency(), rc.maxFrequency ) ;
			rc.minFrequency = Math.min( neurons[i].frequency(), rc.minFrequency ) ;
		}

		int numEdges = 0 ;
		for( int i=0 ; i<targetEdges.length ; i++ ) {
			numEdges += targetEdges[i].size() ;
		}
		
		rc.edges = new EdgeState[ numEdges ] ;
		int ix = 0 ;
		for( int i=0 ; i<targetEdges.length ; i++ ) {
			EdgeList el = targetEdges[i] ;
			for( int j=0 ; j<el.size() ; j++ ) {
				rc.edges[ix++] = new EdgeState( el.get(j) ) ;
			}
		}
		return rc ;
	}


	public CharSequence toJson() {

		StringBuilder rc  = new StringBuilder() ;
		rc.append( "{ \"nodes\": [") ;
		printNodes( rc ) ;
		rc.append( "] ,") ;

		rc.append( "\"links\": [") ;
		printLinks( rc ) ;
		rc.append( "] }") ;

		return rc ;
	}

	private boolean tooBigToPrint() {
		return neurons.length > 200000 ;
	}
	private void printNodes( StringBuilder rc ) {
		char sep = ' '  ;
		int layerWidth = 800 / ( layerSizes.length - 1 );
		int n = 0 ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			n = Math.max( n,  layerSizes[i] ) ;
		}

		for( int i=0 ; i<neurons.length ; i++ ) {
			boolean shouldDrawThisOne = !tooBigToPrint() || i==0 || i==(neurons.length - 1) ;

			if( shouldDrawThisOne ) {
				int x = getLayerFromIndex(i) ;
				int y = i - getIndexOfFirstInLayer(x) ;
				int layerHeight = (600-60) / ( layerSizes[x] );

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
			}
		}
	}

	private void printLinks( StringBuilder rc ) {
		char sep = ' '  ;
		if( !tooBigToPrint() ) {
			for( int i=0 ; i<neurons.length ; i++  ) {
				for( int e=0 ; e<targetEdges[i].size() ; e++ ) {
					Edge edge = targetEdges[i].get(e) ;
					int sourceIndex = edge.source() ;
					Neuron target = neurons[i] ;
		
					rc.append( sep ) 
						.append( "{\"source\":" ) 		
						.append( sourceIndex ) 
						.append( ",\"target\":" ) 
						.append( target.getId() ) 
						.append( ",\"weight\":" ) 
						.append( edge.weight() ) 		
						.append( ",\"id\":\"" ) 
						.append( edge.id() ) 		
						.append( "\" }" ) 
						;
						sep = ',' ;
				}
			}
		}
	}


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

	public int getNumLayers() {
		return neurons.length ;
	}


	public Neuron getNeuron( int id ) {
		return id>=0 && id<neurons.length ? neurons[id] : null ;
	}
	
	public EdgeList getIncomingEdges( int target ) {
		return targetEdges[target] ;
	}
	
	public EdgeList getOutgoingEdges( int source ) {
		EdgeList rc = new EdgeList() ;
		
		for( EdgeList el : targetEdges ) {
			for( Edge e : el ) {
				if( e.source() == source ) {
					rc.add( e ) ; 
				}
			}
		}
		return rc ;
	}

	
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
		return numNeurons ;
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
	public double maxFrequency ;
	public double minFrequency ;
	public List<NeuronState> neurons ;
	public EdgeState edges[] ;
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

class EdgeState {
	public String id ;
	public double weight ;
	public EdgeState( Edge e ) {
		this.weight = e.weight() ;
		this.id = e.id() ;
	}
}

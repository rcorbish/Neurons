package com.rc ;

import java.util.*;

import org.jtransforms.fft.DoubleFFT_1D;
import org.la4j.Vector;
import org.la4j.matrix.sparse.CCSMatrix;

import org.la4j.vector.dense.BasicVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.neurons.InputNeuron;
import com.rc.neurons.Neuron;
import com.rc.neurons.NeuronFactory;
import com.rc.neurons.NeuronRS;

public class Brain  {

	private final static Logger log = LoggerFactory.getLogger( Brain.class ) ;

	private static final Random rng = new Random(24) ;	// utility random number generator

    private final static int HISTORY_LENGTH = 1024 ;
    private final static int WINNER_TAKE_ALL_WIDTH = 7 ;

	// Used to calc rands with the following stats
	private final static double WEIGHT_MEAN = 0.7 ;
    private final static double WEIGHT_SIGMA = 0.15 ;
	
	private final double outputHistory[] ;
	private final boolean outputSpikeHistory[] ;
	private int historyIndex ;
	private int followingId ;
	
	private final CCSMatrix synapses ;		// adjacency matrix

	private final int numColumns ;
	private final int numRows ;
	private final Neuron neurons[] ;			// neurons in each layer
	private final Neuron inputNeurons[] ;			// neurons in each layer
	private final Neuron outputNeurons[] ;			// neurons in each layer
    private final int winnerTakeAllLayers[] ;       // which layers in the network are winner take all

    private double clock ;
	private final double tickPeriod ;
	private final double connectionProbability ;

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
	 * 
	 * @param tickPeriod the period of a clock tick
	 * @param connectionProbability describes synapse density 0.0 .. 1.0
     * @param numInputs the number of input neurons
     * @param numOutputs the number of output neurons
     * @param rows the number of rows in the liquid
     * @param cols the number of cols in the liquid
	 */
	public Brain( double tickPeriod, double connectionProbability, int numInputs, int numOutputs, int rows, int cols ) {

		this.tickPeriod = tickPeriod ;
		this.connectionProbability = connectionProbability ;
		
		this.clock = 0 ;

		this.outputHistory = new double[HISTORY_LENGTH] ; 
		this.outputSpikeHistory = new boolean[HISTORY_LENGTH] ;
		this.historyIndex = 0 ;

		this.numColumns = cols ;
		this.numRows = rows ;

		this.neurons = new Neuron[rows*cols] ;
		this.inputNeurons = new Neuron[numInputs] ;
		this.outputNeurons = new Neuron[numOutputs] ;

		this.winnerTakeAllLayers = new int[ (cols-WINNER_TAKE_ALL_WIDTH-1) / WINNER_TAKE_ALL_WIDTH ] ;
		for( int i=0 ; i<winnerTakeAllLayers.length ; i++ ) {
		    winnerTakeAllLayers[i] = (i+1) * WINNER_TAKE_ALL_WIDTH ;
        }

		// fill with liquid neurons
        for( int i=0 ; i<this.neurons.length ; i++ ) {
            this.neurons[i] = NeuronFactory.getNeuron( i ) ;
        }

        // overwrite the inputs in the liquid
        int dy = rows / numInputs ;
		int ix = 0 ;
		for( int i=0 ; i<this.inputNeurons.length ; i++, ix+=dy ) {
			this.inputNeurons[i] = new InputNeuron( ix ) ;
			this.neurons[ix] = this.inputNeurons[i] ;
		}

		// overwrite the outputs in the liquid
        dy = rows / numOutputs ;
        ix = cols * rows - dy ;
		for( int i=0 ; i<this.outputNeurons.length ; i++, ix-=dy ) {
			this.outputNeurons[i] = new NeuronRS( ix ) ;
			this.neurons[ix] = this.outputNeurons[i] ;
		}

        this.synapses = new CCSMatrix(neurons.length,neurons.length,0) ;

		connectLayers() ;
		this.train = false ;
		this.fftSpike = false ;
		this.fft = null ;
	}


	/**
	 * Connects neurons in each layer, to the previous layer
	 * 
	 */
	private void connectLayers() {

        int fromColumn = 0 ;
        int fromRow = 0 ;

        for( int from=0 ; from<numNeurons() ; from++ ) {
            Neuron src = getNeuron( from ) ;

            int toColumn = 0 ;
            int toRow = 0 ;

            for( int to=0 ; to<numNeurons() ; to++ ) {
                double dist = Math.sqrt(
                                    (fromColumn - toColumn) * (fromColumn - toColumn) +
                                    (fromRow - toRow) * (fromRow - toRow)
                                ) ;
                if( dist > 0 && fromColumn <= toColumn ) {   // no self connections
					double p = connectionProbability * gammaPDF( dist ) ;
					if( rng.nextDouble() < p ) {
						synapses.set( to, from, getRandomWeight() ) ;
					}
                }
                toRow++ ;
                if( toRow>=numRows ) {
                    toColumn++ ;
                    toRow = 0 ;
                }
			}

            fromRow++ ;
            if( fromRow>=numRows ) {
                fromColumn++ ;
                fromRow = 0 ;
            }
        }
		log.info( "Created {} synapses", synapses.cardinality() ) ;
	}

//    private static final int ALPHA = 4 ;
//    private static final double BETA = .2 ;
    private static final int K = 3 ;
    private static final double THETA= .7 ;

	//
	//	https://en.wikipedia.org/wiki/Gamma_distribution
	//
	private double gammaPDF( double x ) {
//        double rc =
//                ( Math.pow( BETA, ALPHA ) / gamma( ALPHA ) ) *
//                  Math.pow( x, ALPHA-1 ) * Math.exp( -BETA * x )
//                ;
        double rc =
                ( Math.pow( x, K-1 ) * Math.exp( -x / THETA ) )
                        /
                ( gamma( K ) * Math.pow( THETA, K ) )
                ;
		return rc ;
	}


	//
	// Gamma function is (n-1)! if n is an integer
	private int gamma( int x ) {
		int rc = 1 ;
		// NB factorial would be i<=x
		for( int i=1 ; i<x ; i++  ) {
			rc *= i ;
		}
		return rc ;
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

		// Will build up all outputs - before changing any of them
		double newPotentials[] = calculateNewPotentials() ;

		// Set inputs immediately - no dependencies
		 for( int i=0 ; i<inputNeurons.length ; i++ ) {
			newPotentials[ inputNeurons[i].getId() ] = inputs[i] ;
		}

		// Then write the output as an atomic op
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].step( newPotentials[i], clock ) ;
		}
		
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].checkForSpike(clock) ;
		}


		for( int i=0 ; i<winnerTakeAllLayers.length ; i++ ) {
            winnerTakeAll( winnerTakeAllLayers[i] ) ;
        }

	}


    protected void winnerTakeAll( int layer ) {

        int ix = layer * getRows() ;
        int maxIndex = ix ;
        double maxPotential = getNeuron(ix).getPotential() ;
        for( int j=0 ; j<getRows() ; j++, ix++ ) {
            if( getNeuron(ix).getPotential() > maxPotential ) {
                maxPotential = getNeuron(ix).getPotential() ;
                maxIndex = ix ;
            }
        }

        if( getNeuron(maxIndex).isSpiking() ) {
            ix = layer * getRows();
            for (int j = 0; j < getRows(); j++, ix++) {
                if (ix != maxIndex) {
                    getNeuron(ix).reset();
                }
            }
        }
    }



    /**
     * Calculate the potential of each output neuron. This is
     * going to be 0.35 for each spiking neuron scaled by each
     * weight.
	 * 	
	 * Weight Matrix is an adjacency matrix A
	 * Neuron vector n is the neuron potentials
	 *	
	 *	in A
	 *		rows represent the source ( i.e. from )
	 *		columns represent the target ( i.e. to )
	 *	
	 *	transfer across the grid =
	 *		A * n   ( adjacency x neuron outputs )
     *
     * @return return an array of output potentials (1 per neuron )
     */
	protected double[] calculateNewPotentials() {

        Vector neu = new BasicVector( neurons.length ) ;

		for( int i=0 ; i<neurons.length ; i++ ) {
			neu.set( i, neurons[i].isSpiking() ? 0.35 : 0 ) ;
		}

		Vector res = synapses.multiply( neu ) ;

		double rc[] = new double[ res.length() ] ;
		for( int i=inputNeurons.length ; i<rc.length ; i++ ) {
		    rc[i] = res.get(i) ;
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

	public Neuron[] getInputsTo( int id ) {
	    Neuron rc[] = new Neuron[ numNeurons() ] ;

	    int n = 0 ;
	    for( int c=0 ; c<numNeurons() ; c++ ) {
	        if( synapses.nonZeroAt( id, c ) ) {
				rc[n] = neurons[c] ;
				n++ ;
            }
        }

	    return Arrays.copyOf( rc, n ) ;
    }


    public void addWeight( int from, int to, double addition ) {
	    if( !synapses.nonZeroAt( to, from ) ) {
	        log.warn( "Warning editing non existant weight {} -> {}", from, to ) ;
        } else {
	        double v = synapses.get( to, from ) + addition ;
            if( v < 0.00 ) {
                v = 0.00 ;
                log.debug( "Weight is 0: {} -> {}", from, to ) ;
            }
            if( v > 1.00 ) v = 1.00 ;
	        synapses.set( to, from, v );
        }
    }

	/**
	 * If we are following - maintain a history of output
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
					tmp[i] = outputSpikeHistory[i] ? 1 : 0 ;
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
				rc.history[i] = ( tmp[i] + 10 ) / 20.0 ;
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


	public Object toJson() {

	    Nodes rc = new Nodes() ;
	    rc.nodes = 	getNodes() ;

		return rc ;
	}

	private Node [] getNodes() {

		int layerWidth = 800 / 80 ;  // cols ?

        Node rc[] = new Node[ neurons.length ] ;

		int x = 0 ;
		int y = 0 ;
        int layerHeight = (600-60) / numRows ;

		for( int i=0 ; i<neurons.length ; i++ ) {
            rc[i] = new Node() ;
            rc[i].id = neurons[i].getId() ;
            rc[i].potential = neurons[i].getPotential() ;
            rc[i].inhibitor = neurons[i].isInhibitor() ;
            rc[i].alive = true ; //routeToAnyInput( i ) ;
            rc[i].fx =  x * layerWidth ;
            rc[i].fy =  y * layerHeight + 30 /* *30+30 */ ;
			y++ ;
			if( y >= numRows ) {
				x++ ;
				y = 0 ;
			}
		}
		return rc ;
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


    /**
     * Determine whether a route exists to an input node
     * from the given node.
     *
     * @param id the node from which to start the search
     */
    public boolean routeToAnyInput( final int id ) {
        Queue<Integer> queue = new LinkedList<>() ;
        Set<Integer> visited = new HashSet<>() ;
		int route[] = new int[ numNeurons() ] ;
        Arrays.fill( route, -1 ) ;

        queue.add( id ) ;
        visited.add( id ) ;

        Set<Integer> inputIds = new HashSet<>() ;
        for( int i=0 ; i<inputNeurons.length ; i++ ) {
            inputIds.add( inputNeurons[i].getId() ) ;
        }

        while( !queue.isEmpty() ) {
            int n = queue.poll() ;
            if( inputIds.contains(n) ) {
				int ix = n ;
				List<Integer> path = new ArrayList<>() ;
				while( ix != -1 ) {
					path.add( ix ) ;
					ix = route[ix] ;
				}
				log.info( "Path: {}", path ) ;
                return true ;
            }
            for( int i=0 ; i<neurons.length ; i++ ) {
                if( synapses.nonZeroAt( n, i ) && !visited.contains(i) ) {
                    queue.add( i ) ;
					visited.add( i ) ;
					route[i] = n ;
                }
            }
        }
//        log.info( "No route to any input from {}", id ) ;
        return false ;
    }


	public Neuron getNeuron( int id ) {
		return id>=0 && id<neurons.length ? neurons[id] : null ;
	}
	
	public int getNumInputs() { return inputNeurons.length ; } 
	public int getNumOutputs() { return outputNeurons.length ; }
	public int getRows() { return numRows ; }
	public int getColumns() { return numColumns ; }

	public void setFollowing(int following) {
		if( this.followingId != following ) {
			Neuron n = getNeuron( following ) ;
			if( n != null ) {
				log.info( "Following: {}", n ) ;
				Neuron sources[] = getInputsTo( following ) ;
				for( Neuron src : sources ) {
					log.info( "Weight from {} is {}", src.getId(), synapses.get(following, src.getId() ) ) ;
				}
			}
			Arrays.fill( outputHistory, 0 ) ;
			Arrays.fill( outputSpikeHistory, false ) ;
            routeToAnyInput(following);
 		}

// 		EigenDecompositor ed = new EigenDecompositor( synapses ) ;
//        Matrix vd[] = ed.decompose() ;

		this.followingId = following ;
	}	
	

	public double clock() {
		return clock ;
	}

    public int numNeurons() {
        return neurons.length ;
    }
    public CCSMatrix getSynapses() {
        return synapses ;
    }

	public boolean isTrain() {
		return train;
	}

	public void setTrain(boolean train) {
		this.train = train ;
		/*
		if( train == false ) {
			CCSMatrix newSynapses = new CCSMatrix( synapses.rows(), synapses.columns() ) ;
			for( int r=0 ; r<newSynapses.rows() ; r++ ) {
				for( int c=0 ; c<newSynapses.columns() ; c++ ) {
					if( synapses.get(r,c) != 0.0 ) {
						newSynapses.set(r, c, synapses.get(r,c) ) ;
					}
				}
			}
			synapses = newSynapses ;
		}
		*/
		log.info( "Cardinality {}", synapses.cardinality() ) ;
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

class Node {
    int id;
    double potential;
    boolean inhibitor ;
    boolean alive ;
    double fx;
    double fy;
}

class Nodes {
    Node [] nodes ;
}
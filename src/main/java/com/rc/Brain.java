package com.rc ;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.*;

import com.rc.neurons.*;
import org.ejml.data.*;
import org.ejml.ops.SortCoupledArray_F64;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.jtransforms.fft.DoubleFFT_1D;

//import org.la4j.Vector;
//import org.la4j.matrix.sparse.CCSMatrix;
//import org.la4j.vector.dense.BasicVector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Brain  {

	private final static Logger log = LoggerFactory.getLogger( Brain.class ) ;

	private static final Random rng = new Random(24) ;	// utility random number generator

	private final static int HISTORY_LENGTH = 1024 ;

	private final static int EPOCH_LENGTH = 100 ;
    private final static double WINNER_TAKE_ALL_RADIUS = 2 ;
    private final static double CONNECTION_DENSITY = 0.70 ;

    // Used to calc rands with the following stats
	private final static double WEIGHT_MEAN = 0.4 ;
    private final static double WEIGHT_SIGMA = 0.25 ;
	
	private final double outputHistory[] ;
	private final boolean outputSpikeHistory[] ;
	private int historyIndex ;
	private int followingId ;
	
    private final DMatrixSparseCSC synapses ;
    private final DMatrixSparseCSC training ;

	private final int numColumns ;
	private final int numRows ;
	private final Neuron neurons[] ;			// neurons in each layer
	private final InputNeuron inputNeurons[] ;			// neurons in each layer
	private final OutputNeuron outputNeurons[] ;			// neurons in each layer

	private double clock ;
	private int epoch ;
	private final double tickPeriod ;
	private final double connectionProbability ;

	private double runningScore ;

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

        this.numRows = g.getInt( 0 ) ;
        this.numColumns = g.getInt( 1 ) ;

		int numNeurons = numRows * numColumns ;
        this.neurons = new Neuron[numNeurons] ;

        this.inputNeurons = new InputNeuron[ g.getInt(2) ] ;
        this.outputNeurons = new OutputNeuron[ g.getInt(3) ] ;

        int ix = 4 ;

        for( int i=0 ; i<neurons.length ; i++ ) {
            int t = g.getInt( ix++ ) ;
            NeuronType type = NeuronType.fromOrdinal( t ) ;
            try {
                neurons[i] = NeuronFactory.getNeuron(type, i);
            } catch( Exception e ) {
                neurons[i] = new NeuronRS( i ) ;
            }
        }
        this.connectionProbability = 1.0 + g.getDouble( ix++ ) ;

		// TODO put this back - it's important
		/*
		// overwrite the inputs in the liquid
        int dy = getRows() / getNumInputs() ;
        int inputIndex = 0 ;
        for( int i=0 ; i<this.inputNeurons.length ; i++, inputIndex+=dy ) {
            this.inputNeurons[i] = this.neurons[inputIndex] ;
        }

        // overwrite the outputs in the liquid
        dy = getRows() / getNumOutputs() ;
        int outputIndex = neurons.length - dy ;

        for( int i=0 ; i<this.outputNeurons.length ; i++, outputIndex-=dy ) {
            this.outputNeurons[i] = this.neurons[outputIndex] ;
        }
		*/

        this.synapses = new DMatrixSparseCSC( neurons.length, neurons.length,0) ;
        this.training = new DMatrixSparseCSC( neurons.length, neurons.length,0) ;

        // connectLayers() ;

		int numConnections = g.getInt( ix++ ) ;
		for( int i=0 ; i<numConnections ; i++ ) {
			int r = g.getInt( ix++ ) ;
			int c = g.getInt( ix++ ) ;
			double w = g.getDouble( ix++ ) ;
			log.debug("{}, Setting {}, {} to {}", ix, r,c,w ) ;
			synapses.set(r, c, w) ;
		}

        this.fftSpike = false ;
        this.fft = null ;

        this.outputHistory = new double[HISTORY_LENGTH] ;
		this.outputSpikeHistory = new boolean[HISTORY_LENGTH] ;
	}


	/**
	 * save the brain as a coded sequence. This is used
	 * during evolution.
	 * 
	 */
	public Genome toGenome() {
		Genome rc = new Genome() ;

        rc.set( getRows() ) ;
        rc.set( getColumns() ) ;
        rc.set( inputNeurons.length ) ;
        rc.set( outputNeurons.length ) ;

        for( int i=0 ; i<neurons.length ; i++) {
			rc.set( neurons[i].getType().ordinal() ) ;
		}

		rc.set( connectionProbability - 1.0  ) ;

		rc.set( synapses.nz_length ) ;

		eachNonZero( (i,j,v) -> {
           rc.set( i ) ;
           rc.set( j ) ;
		   rc.set( v ) ;
		   log.debug( "{} Saving {}, {} = {}", rc.capacity(), i,j,v) ;
 		}) ;
		
		log.info( "Genome length: {} ", rc.capacity() ) ;
		return rc ;
	}


	/**
	 * Create a new instance of a brain.
	 * 
	 * @param tickPeriod the period of a clock tick
     * @param numInputs the number of input neurons
     * @param numOutputs the number of output neurons
     * @param rows the number of rows in the liquid
     * @param cols the number of cols in the liquid
	 */
	public Brain( double tickPeriod, int numInputs, int numOutputs, int rows, int cols ) {

		this.tickPeriod = tickPeriod ;
		this.connectionProbability = 1.0+CONNECTION_DENSITY ;
		
		this.clock = 0 ;

		this.outputHistory = new double[HISTORY_LENGTH] ; 
		this.outputSpikeHistory = new boolean[HISTORY_LENGTH] ;
		this.historyIndex = 0 ;

		log.info( "Creating matrix {} x {}", rows, cols ) ;
		this.numColumns = cols ;
		this.numRows = rows ;

		this.neurons = new Neuron[rows*cols] ;
		this.inputNeurons = new InputNeuron[numInputs] ;
		this.outputNeurons = new OutputNeuron[numOutputs] ;

		// fill liquid with neurons
        for( int i=0 ; i<this.neurons.length ; i++ ) {
			this.neurons[i] = NeuronFactory.getNeuron( i ) ;
        }

        // overwrite the inputs in the liquid
		int start = ( rows - numInputs ) >> 1 ;
		int ix = start ;
		for( int i=0 ; i<this.inputNeurons.length ; i++, ix++ ) {
            this.inputNeurons[i] = new InputNeuron( ix ) ;
			this.neurons[ix] = this.inputNeurons[i] ;
		}

		// overwrite the outputs in the liquid
		start = ( rows - numOutputs ) >> 1 ;
		ix = (cols-1) * rows + start ;

		for( int i=0 ; i<this.outputNeurons.length ; i++, ix++ ) {
			this.outputNeurons[i] = new OutputNeuron( ix ) ;
			this.neurons[ix] = this.outputNeurons[i] ;
		}

        this.synapses = new DMatrixSparseCSC( neurons.length, neurons.length,0) ;
        this.training = new DMatrixSparseCSC( neurons.length, neurons.length,0) ;

		connectLayers() ;
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
			fromColumn = from / numRows ;
            int toColumn = fromColumn+1 ;
            int toRow = 0 ;

            for( int to=toColumn*numRows ; to<numNeurons() ; to++ ) {
                double dist = Math.sqrt(
                                    (fromColumn - toColumn) * (fromColumn - toColumn) +
                                    (fromRow - toRow) * (fromRow - toRow)
                                ) ;
                if( dist > 0 ) {   // no self connections
					double p = gammaPDF( dist / connectionProbability ) ;
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
	}

//    private static final int ALPHA = 4 ;
//    private static final double BETA = .2 ;
    private static final int K = 3 ;
    private static final double THETA= .3 ;

	//
	//	https://en.wikipedia.org/wiki/Gamma_distribution
	//
	private double gammaPDF( double x ) {
//        double rc =
//                ( Math.pow( BETA, ALPHA ) / gamma( ALPHA ) ) *
//                  Math.pow( x, ALPHA-1 ) * Math.exp( -BETA * x )
//                ;
        return
                ( Math.pow( x, K-1 ) * Math.exp( -x / THETA ) )
                        /
                ( gamma( K ) * Math.pow( THETA, K ) )
                ;
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
			newPotentials[ inputNeurons[i].getId() ] = inputs[i] / 3.0 ;
		}

		// Then write the output as an atomic op
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].step( newPotentials[i], clock ) ;
		}
	}



    public double distanceBetween( Neuron n1, Neuron n2 ) {
        int x1 = n1.getId() % getRows() ;
        int y1 = n1.getId() / getRows() ;
        int x2 = n2.getId() % getRows() ;
        int y2 = n2.getId() / getRows() ;

        return Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) ) ;
    }


    /**
     * Get neurons that are within the given distance from a centre.
     * The distance metric is not cartesian, the x ( inter layer)
     * distance is 3x greater than the distance within the same
     * layer.
     *
     * @param n1 The centre neuron
     * @param distance the metric distance to use as a max.
     * @return a List of @see Neuron
     */
    public List<Neuron> neuronsCloseTo( Neuron n1, double distance ) {
	    List<Neuron> rc = new ArrayList<>() ;

        int x1 = n1.getId() / getRows() ;
        int y1 = n1.getId() % getRows() ;

        // Find the AABB
        int minx = (int)Math.max( x1 - distance, 0 ) ;
        int maxx = (int)Math.min( x1 + distance, getColumns()-1 ) ;
        int miny = (int)Math.max( y1 - distance, 0 ) ;
        int maxy = (int)Math.min( y1 + distance, getRows()-1 ) ;

        // Scan the AABB for all elements - then refine the check
        for( int x=minx ; x<=maxx ; x++ ) {
            // Assume that the x distance is 3x greater than y distance
            int dx2 = 9 * ( x - x1 ) * ( x - x1 ) ;
            int ix = x * getRows() + miny ;
            for( int y=miny ; y<=maxy ; y++ , ix++) {
                int dy2 = ( y - y1 ) * ( y - y1 ) ;
                if( ix != n1.getId() && Math.sqrt( dx2 + dy2 ) <= distance ) {
                    rc.add( neurons[ix] ) ;
                }
            }
        }
        return rc ;
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

        DMatrixRMaj neuronOutputs = new DMatrixRMaj(  neurons.length, 1 ) ;

		for( int i=0 ; i<neurons.length ; i++ ) {
			double v = 0.0 ;
			if( neurons[i].isSpiking() ) {
			    // spike value is -ve for inhibitors
				v = neurons[i].getSpikeValue() ;
			}
			neuronOutputs.set( i, v ) ;
		}

        DMatrixRMaj rc = new DMatrixRMaj( neurons.length, 1 ) ;
        CommonOps_DSCC.mult( synapses, neuronOutputs, rc ) ;

        return rc.getData() ;
	}

	
	/**
	 * Train all neurons
     *
	 */
	public void train( int y ) {

		for( int i=0 ; i<this.outputNeurons.length ; i++ ) {
			this.outputNeurons[i].setSupervisedFiring( i==y ) ;
		}

		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].train( this, training ) ;
		}
		
		epoch++ ;
		if( epoch == EPOCH_LENGTH ) {
			epoch = 0 ;
			IGrowArray ig = new IGrowArray() ;
			DGrowArray dg = new DGrowArray() ;

			CommonOps_DSCC.add( 1.0, synapses, 1.0, training, synapses, ig, dg ) ;

			eachNonZero( (i,j,v) -> {
				if( v>1.0 ) synapses.set(i, j, 1.0);
				if( v<=0.0 ) synapses.set(i, j, 0.0);
			});

			log.debug( "Train sum,max     {}, {}", CommonOps_DSCC.elementSum(training), CommonOps_DSCC.elementMax(training) ) ;
			log.debug( "Synapse sum       {}", CommonOps_DSCC.elementSum(synapses) ) ;

			training.zero();
			synapses.sortIndices( new SortCoupledArray_F64() ) ;
			synapses.shrinkArrays();
		}
		runningScore += getScore( y ) ;
	}


	public Neuron[] getInputsTo( int id ) {
	    Neuron rc[] = new Neuron[ numNeurons() ] ;

	    int n = 0 ;
	    for( int c=0 ; c<numNeurons() ; c++ ) {
	        if( synapses.isAssigned( id, c ) ) {
				rc[n] = neurons[c] ;
				n++ ;
            }
        }
	    return Arrays.copyOf( rc, n ) ;
    }

	public Neuron[] getOutputsFrom( int id ) {
	    Neuron rc[] = new Neuron[ numNeurons() ] ;

	    int n = 0 ;
	    for( int r=0 ; r<numNeurons() ; r++ ) {
	        if( synapses.isAssigned( r, id ) ) {
				rc[n] = neurons[r] ;
				n++ ;
            }
        }

	    return Arrays.copyOf( rc, n ) ;
    }


    public void addWeight( int from, int to, double addition ) {
	    if( !synapses.isAssigned( to, from ) ) {
	        log.warn( "Warning editing non existant weight {} -> {}", from, to ) ;
        } else {
	        double v = synapses.get( to, from ) + addition ;
            if( v < 0.00 ) {
                v = 0.00 ;
//                log.debug( "Weight is 0: {} -> {}", from, to ) ;
            }
            if( v > 10.00 ) v = 10.00 ;
	        synapses.set( to, from, v );
        }
    }


    public void addTraining( int from, int to, double addition ) {
		double v = training.get( to, from ) + addition ;
		// if( v < 0.00 ) {
		// 	v = 0.00 ;
		// }
		// if( v > 1.00 ) v = 1.00 ;
		training.set( to, from, v );
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
	private double getScore( int y ) {
	    double rc = outputNeurons.length  ;

		for( int i=0 ; i<outputNeurons.length ; i++ ) {
			outputNeurons[i].updateFrequency( clock ) ;
			log.debug( "O/P[{}] => {} Hz", i, outputNeurons[i].frequency() ) ; 
		    if( i==y ) {
                rc += outputNeurons[i].frequency() * 100.0 ;
            } else {
                rc -= outputNeurons[i].frequency() ;
            }
        }
	    return rc ;
	}


    public double getSummaryScore() {
        return runningScore ;
    }

    public void resetSummaryScore() {
        runningScore = 0.0 ;
    }

	public void resetNeurons() {
		for( int i=0 ; i<neurons.length ; i++ ) {
			neurons[i].reset() ;
		}
	}


	public Potentials getNeuronPotentials( double clock ) {
		Potentials rc = new Potentials() ;
		rc.clock = clock ;

		rc.frequency = followingId >0 ? neurons[followingId].frequency() : 0 ;
		rc.history = new double[outputHistory.length] ;
		rc.spikeHistory = new boolean[outputHistory.length] ;

		int offset = historyIndex ;

		double mx = -1e10 ;
		double mn = 1e10 ;

		if( isFourier() ) {
			double tmp[] = new double[ outputHistory.length ] ;
			if( this.fftSpike ) {
				for( int i=0 ; i<tmp.length ; i++ ) {
					tmp[i] = outputSpikeHistory[i] ? 100 : 0 ;
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
				rc.history[i] =  tmp[i] ;
				rc.spikeHistory[ix] = outputSpikeHistory[i] ;
				if( mn>-300) mn = Math.min( mn, tmp[i] ) ;
                if( mn<300) mx = Math.max( mx, tmp[i] ) ;
			}
		} else {
			for( int i=0 ; i<outputHistory.length ; i++ ) {
				offset-- ;
				if( offset<0 ) {
					offset += outputHistory.length ;
				}
				int ix = outputHistory.length-offset-1 ;
				rc.history[ix] = outputHistory[i]  ;
				rc.spikeHistory[ix] = outputSpikeHistory[i] ;
                mn = Math.min( mn, outputHistory[i] ) ;
                mx = Math.max( mx, outputHistory[i] ) ;
			}
		}
        rc.min = mn ;
        rc.max = mx ;
		rc.neurons = new ArrayList<NeuronState>( neurons.length ) ;
		for( int i=0 ; i<neurons.length; i++ ) {
			rc.neurons.add( new NeuronState( neurons[i], clock ) ) ;
		}
		rc.outputs = new double[outputNeurons.length][] ;
		rc.outputIndex = new int[outputNeurons.length] ;
		for( int i=0 ; i<rc.outputs.length ; i++ ) {
			rc.outputs[i] = outputNeurons[i].getHistory() ;
			rc.outputIndex[i] = outputNeurons[i].getHistoryIndex() ;
		}

		rc.inputs = new double[inputNeurons.length][] ;
		rc.inputIndex = new int[inputNeurons.length] ;
		for( int i=0 ; i<rc.inputs.length ; i++ ) {
			rc.inputIndex[i] = inputNeurons[i].getHistoryIndex() ;
			rc.inputs[i] = inputNeurons[i].getHistory() ;
		}
		
		return rc ;
	}


	public Object toJson() {

	    Nodes rc = new Nodes() ;
        rc.cols = numColumns ;
        rc.rows = numRows ;
	    rc.nodes = 	getNodes() ;
		return rc ;
	}

	private Node [] getNodes() {

        Node rc[] = new Node[ neurons.length ] ;

		int x = 0 ;
		int y = 0 ;

		for( int i=0 ; i<neurons.length ; i++ ) {
            rc[i] = new Node() ;
            rc[i].id = neurons[i].getId() ;
            rc[i].potential = neurons[i].getPotential() ;
            rc[i].inhibitor = neurons[i].isInhibitor() ;
            rc[i].alive = true ; //routeToAnyInput( i ) ;
            rc[i].fx =  x ;
            rc[i].fy =  y ;
			y++ ;
			if( y >= numRows ) {
				x++ ;
				y = 0 ;
			}
		}
		return rc ;
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
			rc = null ; //new Brain( tick, g ) ;
		} catch( Exception ioe ) {
			rc = null ;
			log.warn( "Failed loading brain from {}", ioe.getMessage() );
		}
		return rc ;
	}		



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
                if( synapses.isAssigned( n, i ) && !visited.contains(i) ) {
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
    public DMatrixSparseCSC getSynapses() {
        return synapses ;
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


	public void eachNonZero( MatrixVisitor visitor ) {
        for(int col = 0; col < synapses.numCols; ++col) {
            int idx0 = synapses.col_idx[col];
            int idx1 = synapses.col_idx[col + 1];

            for(int i = idx0; i < idx1; ++i) {
                int row = synapses.nz_rows[i];
                double value = synapses.nz_values[i];

                visitor.apply( row, col, value ) ;
            }
        }
    }


    @FunctionalInterface
    public interface MatrixVisitor {
        void apply( int r, int c, double v ) ;
    }
}


class Potentials {
	public double clock ;
	public List<NeuronState> neurons ;
	public double frequency ;
	public double history[] ;
	public boolean spikeHistory[] ;
    public double min ;
	public double max ;
	public int outputIndex[] ;
	public int inputIndex[] ;
	double outputs[][] ;
	double inputs[][] ;
}

class NeuronState {
	public double potential ;
	public int id ;
	public double frequency ;
	public String type ;
	public NeuronState( Neuron n, double clock ) {
		this.potential = n.getPotential() ;
		this.id = n.getId() ;
		this.frequency = n.frequency() ;
		this.type = n.getType().toString() ;
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
	int rows ;
	int cols ;
	Node [] nodes ;
}
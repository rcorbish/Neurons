package com.rc ;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Brain  {

	final static Logger log = LoggerFactory.getLogger( Brain.class ) ;

	private static final Random rng = new Random(24) ;	// utility random number generator

	final static public int HISTORY_LENGTH = 600 ;

	final static double WEIGHT_MEAN = 0.8 ;
	final static double WEIGHT_SIGMA = 0.05 ;
	
	private final double outputHistory[] ;
	private int historyIndex ;
	private int followingId ;
	
	private final EdgeList targetEdges[] ;		// adjacency list ( directed & weighted )

	private final Neuron neurons[] ;			// neurons in each layer
	private final int layerSizes[] ;

	private final int numNeurons ;

	private double clock ;
	private final double tick ;
	
	/**
	 * Create a brain from a genome (bitmask).
	 * 
	 * @param tick the period each clock tick represents
	 * @param g
	 */
	public Brain( double tick, Genome g ) {
		this.tick = tick ;
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
			neurons[i] = new Neuron( gs, i  ) ;
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
	 * @param tick the period of a clock tick
	 * @param dims the number of neurons in each layer
	 */
	public Brain( double tick, int ... layers ) {

		this.tick = tick ;
		this.clock = 0 ;
		this.outputHistory = new double[HISTORY_LENGTH] ; 
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
			this.neurons[i] = new Neuron( i ) ;
		}
		this.numNeurons = numNeurons ;
		this.targetEdges = new EdgeList[ numNeurons ] ;

		connectLayers( 0.5 ) ;		
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
	 * Choose a random weight. Weights may be positive or negative, depending on the
	 * inhibitor ratio (fraction of negative weights)
	 *  
	 * @return
	 */
	protected double getRandomWeight() {
		return rng.nextGaussian() * WEIGHT_SIGMA + WEIGHT_MEAN ;
	}

	/**
	 * Execute one time step, traverse the graph, summing all inputs to each
	 * neuron. Then (atomically) update the weights.
	 * 
	 * @param inputs an array of values to set inputs to
	 */
	public void step( double[] inputs ) {
		clock += tick ;

		// Set inputs immediately - no dependencies
		for( int i=0 ; i<layerSizes[0] ; i++ ) {
			this.neurons[i].step( inputs[i], clock ) ;
		}

		// Will build up all outputs - before changing any of them
		double newPotentials[] = new double[ neurons.length ] ;

		// Sum all inputs 
		for( int i=layerSizes[0] ; i<neurons.length; i++ ) {
			newPotentials[i] = 0 ;
			for( Edge edge : targetEdges[i] ) {
				Neuron src = getNeuron( edge.source() ) ;
				if( src.isSpiking() ) {
					newPotentials[i] += edge.weight() ;
				}
			}
		}

		// Then write the output as an atomic op
		// do NOT write inputs though
		for( int i=1 ; i<neurons.length; i++ ) {
			neurons[i].step( newPotentials[i], clock ) ;
		}		
	}
	
	public void follow() {
		Neuron following = getNeuron( followingId ) ;
		if( following != null ) {
			outputHistory[historyIndex] = following.getPotential() ;
		}
		
		historyIndex++ ;
		if( historyIndex >= outputHistory.length ) {
			historyIndex = 0 ;
		}
	}

	public void train() {
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].train( this, clock ) ;
		}
	}



	/**
	 * Get the overall score 
	 * 
	 * @param y the expected pattern index 
	 */
	public double getScore( int p ) {
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
				if( p==i ) {
					rc += yh ;
				} else {
					rc -= yh ;
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
			return rc ;		// input neuron
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

		int numSpikes = 0 ;
		int offset = historyIndex ;
		for( int i=0 ; i<outputHistory.length ; i++ ) {
			offset-- ;
			if( offset<0 ) {
				offset += outputHistory.length ;
			}
			rc.history[outputHistory.length-offset-1] = outputHistory[i] ;
			if( outputHistory[i] >= 0.90 ) {
				numSpikes++ ;
			}
		}
		rc.frequency = (double)numSpikes / ( outputHistory.length * Main.TICK_PERIOD ) ;

		rc.neurons = new ArrayList<NeuronState>() ;
		for( int i=0 ; i<neurons.length; i++ ) {
			rc.neurons.add( new NeuronState( neurons[i], clock ) ) ;
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
		return neurons.length > 200 ;
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
				rc.append( sep ) 
					.append( "{ \"id\":"  ) 
					.append( neurons[i].getId() ) 
					.append( ",\"potential\":" ) 
					.append( neurons[i].getPotential() ) 
					.append( ",\"fx\":").append( x * layerWidth ) 				
					.append( ",\"fy\":").append( y*30+30 ) 				
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
/*
	public Neuron[] getLayer( int layer ) {
		return neurons[layer] ;
	}

	public Neuron getNeuron( int layer, int ix ) {
		return neurons[layer][ix] ;
	}
*/	
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
		}
		this.followingId = following ;
	}
	
	public double clock() {
		return clock ;
	}

	public int numNeurons() {
		return numNeurons ;
	}
}


class Potentials {
	public double score ;
	public double clock ;
	public double frequency ;
	public List<NeuronState> neurons ;
	public EdgeState edges[] ;
	public double history[] ;
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

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

	final static public int HISTORY_LENGTH = 200 ;

	private final double outputHistory[] ;
	private int historyIndex ;
	private int followingId ;
	
	private final double inhibitorRatio = 0.0 ;
	private final EdgeList targetEdges[] ;		// adjacency list ( directed & weighted )

	private final Neuron neurons[][] ;			// neurons in each layer
 
	/**
	 * Create a brain from a genome (bitmask).
	 * 
	 * @param g
	 */
	public Brain( Genome g ) {
		int numNeurons = 0 ;
		int numlayers  = g.getInt(0) ;

		this.neurons = new Neuron[numlayers][] ;
		
		for( int i=0 ; i<numlayers ; i++ ) {
			int n = g.getInt( i+1 ) ;
			numNeurons += n ;
			neurons[i] = new Neuron[ n ];
		}
		//-----------------------------
		// how many items in the genome
		//
		// 0 = # layers
		// 1 .. n = # neurons in each layer
		int start = 1 + numlayers ;

		// 1st layer is always special (input) neurons
		for( int j=0 ; j<neurons[0].length ; j++ ) {
			Genome gs = g.subSequence(start, Neuron.GENOME_SIZE ) ;
			neurons[0][j] = new InputNeuron( gs ) ;
			start += Neuron.GENOME_SIZE ;	// update Brain size
		}
		// Start from 2nd layer onwards ...
		for( int i=1 ; i<neurons.length ; i++ ) {
			for( int j=0 ; j<neurons[i].length ; j++ ) {
				Genome gs = g.subSequence(start, Neuron.GENOME_SIZE ) ;
				neurons[i][j] = new Neuron( gs ) ;
				start += Neuron.GENOME_SIZE ;	// update Brain size
			}
		}

		targetEdges = new EdgeList[ numNeurons ] ;

		for( int i=0 ; i<targetEdges.length ; i++ ) {
			int numEdges = g.getInt( start ) ;
			Genome gs = g.subSequence(start, numEdges*Edge.GENOME_SIZE + 1 ) ;
			targetEdges[i] = new EdgeList( gs ) ;
			start += numEdges*Edge.GENOME_SIZE + 1 ;
		}

		this.outputHistory = new double[HISTORY_LENGTH] ;
	}

	public Genome toGenome() {
		Genome rc = new Genome() ; 
		rc.set( neurons.length, 0 ) ;
		for( int i=0 ; i<neurons.length ; i++ ) {
			rc.set( neurons[i].length, 1+i ) ;
		}

		for( int i=0 ; i<neurons.length ; i++ ) {
			for( int j=0 ; j<neurons[i].length ; j++ ) {
				rc.append( neurons[i][j].toGenome() ) ;
			}
		}
		
		for( int i=0 ; i<targetEdges.length ; i++ ) {
			rc.append( targetEdges[i].toGenome() ) ;
		}
		return rc ;
	}

	/**
	 * Create a new instance of a brain.
	 * @param numInputs 
	 * @param numOutputs
	 * @param dims the dimensions, up to 3 are relevant
	 */
	public Brain( int ... layers ) {

		this.outputHistory = new double[HISTORY_LENGTH] ; 
		this.historyIndex = 0 ;
		
		this.neurons = new Neuron[layers.length][] ;
				
		int numNeurons = 0 ;
		
		this.neurons[0] = new Neuron[ layers[0] ] ;
		for( int j=0 ; j<layers[0] ; j++ ) {
			this.neurons[0][j] = new InputNeuron( numNeurons ) ;
			numNeurons++ ;
		}

		for( int i=1 ; i<layers.length ; i++ ) {
			this.neurons[i] = new Neuron[ layers[i] ] ;
			for( int j=0 ; j<layers[i] ; j++ ) {
				this.neurons[i][j] = new Neuron( numNeurons ) ;
				numNeurons++ ;
			}
		}
		
		// All neurons have edges 
		this.targetEdges = new EdgeList[ numNeurons ] ;
		for( int i=0 ; i<targetEdges.length ; i++ ) {
			this.targetEdges[i] = new EdgeList() ;
		}
		int edgeId = 100 ;
		

		// Now connect each layer to the previous layer
		for( int l=1 ; l<neurons.length ; l++ ) {
			for( int i=0 ; i<neurons[l].length ; i++ ) {

				int tgtIndex = getIndexOfFirstInLayer(l)+i ; 
				EdgeList el = targetEdges[ tgtIndex ] ;
				
				for( int j=0 ; j<neurons[l-1].length ; j++ ) {
					int dy = Math.abs( i - j ) ;
					if( dy < 3 ) {
						double weight = getRandomWeight() ;
						Edge e = new Edge( getIndexOfFirstInLayer(l-1)+j, tgtIndex, weight, edgeId++ ) ;
						el.add( e ) ;
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
		return rng.nextDouble() * ( rng.nextDouble() < inhibitorRatio ? -1 : 1 ) ;
	}

	/**
	 * Execute one time step, traverse the graph, summing all inputs to each
	 * neuron. Then (atomically) update the weights.
	 * 
	 * @param inputs an array of values to set inputs to
	 */
	public void step( double clock, double[] inputs ) {
		
		// Set inputs immediately - no dependencies
		for( int i=0 ; i<neurons[0].length ; i++ ) {
			this.neurons[0][i].setPotential( inputs[i], clock ) ;
		}

		// Will build up all outputs - before changing any of them
		double newPotentials[][] = new double[ neurons.length ][] ;

		// Sum all inputs 
		int n = neurons[0].length ;
		for( int i=1 ; i<neurons.length; i++ ) {
			newPotentials[i] = new double[ neurons[i].length ] ;
			for( int j=0 ; j<neurons[i].length; j++, n++ ) {
				newPotentials[i][j] = 0 ;
				for( int e=0 ; e<targetEdges[n].size() ; e++ ) {
					Edge edge = targetEdges[n].get(e) ;
					Neuron src = findNeuron( edge.source() ) ;
					if( src.isSpiking() ) {
						newPotentials[i][j] += edge.weight() ;
					}
				}
			}
		}

		// Then write the output as an atomic op
		// do NOT write inputs though
		for( int i=1 ; i<neurons.length; i++ ) {
			for( int j=0 ; j<neurons[i].length; j++ ) {
				neurons[i][j].setPotential( newPotentials[i][j], clock ) ;
			}
		}		

		Neuron following = findNeuron( followingId ) ;
		if( following != null ) {
			outputHistory[historyIndex] = following.getPotential() ;
		}
		
		historyIndex++ ;
		if( historyIndex >= outputHistory.length ) {
			historyIndex = 0 ;
		}
	}

	public void train( double clock ) {
		for( int i=0 ; i<neurons.length; i++ ) {
			for( int j=0 ; j<neurons[i].length; j++ ) {
				neurons[i][j].train( this, clock ) ;
			}
		}
	}

	// High score is better for survival
	public void updateScores() {
	}


	/**
	 * Get the overall score 
	 */
	public double getScore() {
		return 1.0  ;
	}


	/**
	 * Determine in which layer a neuron lies based on its index
	 */
	public int getLayerFromIndex( int index ) {
		
		int rc = -1 ;
		
		if( index < 0 ) {
			return rc ;		// input neuron
		}
		for( rc=0 ; rc<neurons.length ; rc++ ) {
			index -= neurons.length ;
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
		for( int i=0 ; i<Math.min( layer, neurons.length ) ; i++ ) {
			rc += neurons[i].length ;
		}
		return rc ;
	}		


	public Potentials getNeuronPotentials( int patternIndex, double clock ) {
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
			for( int j=0 ; j<neurons[i].length; j++ ) {
				rc.neurons.add( new NeuronState( neurons[i][j], clock ) ) ;
			}
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
		rc.score = patternIndex ;
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
		int layerWidth = 800 / ( neurons.length - 1 );
		int n = 0 ;
		for( int i=0 ; i<neurons.length ; i++ ) {
			n = Math.max( n,  neurons.length ) ;
		}
		int layerHeight = 400 / n - 1 ; 

		for( int i=0 ; i<neurons.length ; i++ ) {
			
			boolean shouldDrawThisOne = !tooBigToPrint() || i==0 || i==(neurons.length - 1) ;

			if( shouldDrawThisOne ) {
				int x = i * layerWidth ;
				for( int j=0 ; j<neurons[i].length ; j++ ) {	
					rc.append( sep ) 
					.append( "{ \"id\":"  ) 
					.append( neurons[i][j].getId() ) 
					.append( ",\"potential\":" ) 
					.append( neurons[i][j].getPotential() ) 
					.append( ",\"fx\":").append( x ) 				
					.append( ",\"fy\":").append( j*30+30 ) 				
					.append( " }" ) 
					;
					sep = ',' ;
				}
			}
			
		}
	}

	private void printLinks( StringBuilder rc ) {
		char sep = ' '  ;
		if( !tooBigToPrint() ) {
			int n=0 ;
			for( int i=0 ; i<neurons.length ; i++  ) {
				for( int j=0 ; j<neurons[i].length ; j++, n++  ) {
					for( int e=0 ; e<targetEdges[n].size() ; e++ ) {
						Edge edge = targetEdges[n].get(e) ;
						int sourceIndex = edge.source() ;
						Neuron target = neurons[i][j] ;
		
						rc.append( sep ) 
						.append( "{\"source\":" ) 		
						.append( sourceIndex ) 
						.append( ",\"target\":" ) 
						.append( target.getId() ) 
						.append( ",\"weight\":" ) 
						.append( edge.weight() ) 		
						.append( ",\"id\":" ) 
						.append( edge.id() ) 		
						.append( " }" ) 
						;
						sep = ',' ;
					}
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


	public static Brain load( String fileName ) {
		Brain rc = null ;
		log.info( "Loading from {}", fileName ) ;
		try ( InputStream is = new FileInputStream( fileName ) ;
				ObjectInputStream ois = new ObjectInputStream(is) )  {
			Genome g = (Genome)ois.readObject() ;
			rc = new Brain( g ) ;
		} catch( Exception ioe ) {
			rc = null ;
			log.warn( "Failed loading brain from {}", ioe.getMessage() );
		}

		return rc ;
	}		

	public int getNumLayers() {
		return neurons.length ;
	}

	public Neuron[] getLayer( int layer ) {
		return neurons[layer] ;
	}

	public Neuron getNeuron( int layer, int ix ) {
		return neurons[layer][ix] ;
	}
	
	public Neuron findNeuron( int id ) {
		for( int i=0 ; i<neurons.length ; i++ ) {
			for( int j=0 ; j<neurons[i].length ; j++ ) {
				if( neurons[i][j].getId() == id ) {
					return neurons[i][j] ;					
				}
			}
		}
		return null ;
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
		this.followingId = following;
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
	public double ago ;
	public NeuronState( Neuron n, double clock ) {
		this.potential = n.getPotential() ;
		this.id = n.getId() ;
		this.ago = n.timeSinceFired( clock ) ;
	}
}

class EdgeState {
	public int id ;
	public double weight ;
	public EdgeState( Edge e ) {
		this.weight = e.weight() ;
		this.id = e.id() ;
	}
}

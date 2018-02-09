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

	final static public int HISTORY_LENGTH = 100 ;
	final public int GENOME_SIZE ;
	private final double outputHistory[][] ;
	private int historyIndex ;

	private final double inhibitorRatio = 0.0 ;
	private final Neuron neurons[] ;
	private final EdgeList edges[] ;			// adjacency list ( directed & weighted )

	private final int layerSizes[] ;			// the size of each layer in the brain
	private final Neuron  [] inputs ;			// which nodes are .. inputs 
	private final Neuron  [] outputs ;			// .. and outputs

	/**
	 * Create a brain from a genome (bitmask).
	 * 
	 * @param g
	 */
	public Brain( Genome g ) {
		int numNeurons = g.getInt( 0 ) ;
		int numInputs = g.getInt( 1 ) ;
		int numOutputs = g.getInt( 2 ) ;

		layerSizes = new int[ g.getInt(3) ] ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			layerSizes[i] = g.getInt( i+4 ) ;
		}
		GENOME_SIZE = 4 + layerSizes.length ;
		
		neurons = new Neuron[ numNeurons ] ;
		inputs  = new Neuron[ numInputs  ] ;
		outputs = new Neuron[ numOutputs ] ;
		int start = 3 ;
		for( int i=0 ; i<numNeurons ; i++ ) {
			Genome gs = g.subSequence(start, Neuron.GENOME_SIZE ) ;
			neurons[i] = new Neuron( gs ) ;
			start += Neuron.GENOME_SIZE ;
		}

		edges = new EdgeList[ numNeurons ] ;

		for( int i=0 ; i<edges.length ; i++ ) {
			int numEdges = g.getInt( start ) ;
			Genome gs = g.subSequence(start, numEdges*Edge.GENOME_SIZE + 1 ) ;
			edges[i] = new EdgeList( gs ) ;
			start += numEdges*Edge.GENOME_SIZE + 1 ;			
		}

		for( int i=0 ; i<numInputs ; i++ ) {
			inputs[i] = neurons[i] ;
		}
		for( int i=0 ; i<numOutputs ; i++ ) {
			outputs[i] = neurons[ numNeurons - i - 1 ] ;
		}

		this.outputHistory = new double[HISTORY_LENGTH][ numOutputs] ; 
	}

	/**
	 * Create a new instance of a brain.
	 * @param numInputs 
	 * @param numOutputs
	 * @param dims the dimensions, up to 3 are relevant
	 */
	public Brain( int numInputs, int numOutputs, int ... dims ) {
		this.outputHistory = new double[HISTORY_LENGTH][ numOutputs] ; 
		this.historyIndex = 0 ;
		
		int numNeurons = numInputs + numOutputs ;
		this.layerSizes = new int[ dims.length ] ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			layerSizes[i] = dims[i] ;
			numNeurons += dims[i] ;
		}
		
		GENOME_SIZE = 4 + layerSizes.length ;

		this.neurons = new Neuron[ numNeurons ] ;
		for( int i = 0 ; i<numNeurons ; i++ ) {
			neurons[i] = new Neuron( i ) ;
		}

		// Remember the inputs & outputs for convenience later 
		this.inputs = new Neuron[numInputs] ;
		this.outputs = new Neuron[numOutputs] ;

		// Copy the first few neurons to input cache
		for( int i=0 ; i<inputs.length ; i++ ) {
			inputs[i] = neurons[i] ;
		}
		// Copy the last few neurons to output cache
		for( int i=0 ; i<outputs.length ; i++ ) {
			outputs[i] = neurons[numNeurons-i-1] ;
		}

		// All neurons can have edges 
		this.edges = new EdgeList[ numNeurons ] ;

		// Connect all from 1st layer to the inputs
		for( int i=0 ; i<layerSizes[0] ; i++ ) {
			EdgeList el = new EdgeList() ;
			edges[i+numInputs] = el ;
			for( int j=0 ; j<numInputs ; j++ ) {
				double weight = getRandomWeight() ;
				Edge e = new Edge( j, weight ) ;
				el.add( e ) ;
			}
		}

		// Connect all from last layer to the outputs
		int lastLayerSize = layerSizes[ layerSizes.length - 1 ];
		int lastLayerStartIndex = numNeurons - numOutputs - lastLayerSize  ;
		for( int i=0 ; i<numOutputs ; i++ ) {
			
			EdgeList el = new EdgeList() ;			
			edges[numNeurons-numOutputs+i] = el ;
			
			for( int j=0 ; j<lastLayerSize ; j++ ) {
				double weight = getRandomWeight() ;
				Edge e = new Edge( lastLayerStartIndex+j, weight ) ;
				el.add( e ) ;
			}
		}

		// Now connect the 'hidden' layers to the previous layer
		for( int l=1 ; l<layerSizes.length ; l++ ) {
			for( int i=0 ; i<layerSizes[l] ; i++ ) {
				EdgeList el = new EdgeList() ;				
				edges[ getIndexOfFirstInLayer(l)+i ] = el ;
				
				for( int j=0 ; j<layerSizes[l-1] ; j++ ) {
					double weight = getRandomWeight() ;
					Edge e = new Edge( getIndexOfFirstInLayer(l-1)+j, weight ) ;
					el.add( e ) ;
				}
			}
		}

		// Now connect neighbours in each layer
		for( int l=0 ; l<layerSizes.length ; l++ ) {
			for( int i=0 ; i<layerSizes[l] ; i++ ) {	
				// Get edge list for each node in a layer
				EdgeList el = edges[ getIndexOfFirstInLayer(l)+i ] ;
				if( i>0 ) {
					double weight = getRandomWeight() ;
					Edge e = new Edge( getIndexOfFirstInLayer(l)+i-1, weight ) ;
					el.add( e ) ;
				}
				if( i<layerSizes[l]-1 ) {
					double weight = getRandomWeight() ;
					Edge e = new Edge( getIndexOfFirstInLayer(l)+i+1, weight ) ;
					el.add( e ) ;
				}
			}
		}

		for( int i=0 ; i<numInputs ; i++ ) {
			EdgeList el = new EdgeList() ;
			edges[i] = el ;
		}

	}

	/**
	 * Choose a random weight. Weights may be positive or negative, depending on the
	 * inhibitor ratto (fraction of negative weights)
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
	 * @param clock  the time unit for this step
	 */
	public void step( double[] inputs, int clock ) {
		
		// First decay all weights - each neuron loses 'charge'
		// Do not decay inputs though, they need to fire sometimes
		for( int i=inputs.length ; i<neurons.length ; i++ ) {
			this.neurons[i].decay();
		}

		// Set inputs immediately - no dependencies
		for( int i=0 ; i<inputs.length ; i++ ) {
			this.inputs[i].setPotential( inputs[i], clock );
		}

		// Will build up all outputs - before changing any of them
		double newPotentials[] = new double[ neurons.length ] ;

		// Sum all inputs 
		for( int n=inputs.length ; n<neurons.length; n++ ) {
			newPotentials[n] = 0 ;
			for( int e=0 ; e<edges[n].size() ; e++ ) {
				Edge edge = edges[n].get(e) ;
				newPotentials[n] += neurons[edge.source()].getPotential() * edge.weight() ;
			}
		}

		// Then write the output as an atomic op
		for( int n=inputs.length ; n<neurons.length; n++ ) {
			neurons[n].setPotential( newPotentials[n], clock ) ;
		}
	}

	public void train() {
		for( int n=inputs.length ; n<neurons.length; n++ ) {
			neurons[n].train( this ) ;
		}
	}

	// High score is better for survival
	public void updateScores() {
//		double score = 0 ; //-Math.abs(spikeCost) ;
//		for( int n=0 ; n<outputs.length ; n++  ) {
//			double p = neurons[n].getPotential() ; 
//			score += p ;
//			double tmp = 0 ;
//			for( Neuron o : outputs ) {
//				double dp = o.getPotential() - p ;
//				tmp += dp * dp  ; 
//			}
//			score += Math.sqrt( tmp ) ;
//			double dp = p - getHistory( n, 1 ) ; 
//			score += Math.abs(dp) ;
//		}
	}


	public double getHistory( int outputIndex, int stepsBefore ) {
		int step = historyIndex - stepsBefore ;
		if( step < 0 ) {
			step += outputHistory.length ;
		}
		return outputHistory[step][outputIndex] ;
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
		
		index -= inputs.length  ;
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
		int rc = inputs.length ;
		for( int i=0 ; i<layer ; i++ ) {
			rc += layerSizes[i] ;
		}
		return rc ;
	}		


	public Neuron[] getInputs() { return inputs ; }
	public Neuron[] getOutputs() { return outputs ; }

	public Potentials getNeuronPotentials( int patternIndex, int clk ) {
		Potentials rc = new Potentials() ;

		rc.states = new NeuronState[neurons.length] ;
		rc.outputs = new double[ outputs.length ] ;

		for( int i=0 ; i<outputs.length ; i++ ) {
			rc.outputs[i] = outputs[i].getPotential() ;
		}

		for( int i=0 ; i<neurons.length ; i++ ) {
			rc.states[i] = new NeuronState( neurons[i], clk ) ;
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
		int layerWidth = 800 / ( layerSizes.length + 1 );
		for( int n=0 ; n<neurons.length ; n++ ) {
			boolean shouldDrawThisOne = !tooBigToPrint() || n < inputs.length || n>=(neurons.length - outputs.length) ;

			if( shouldDrawThisOne ) {
				rc.append( sep ) 
				.append( "{ \"id\":"  ) 
				.append( neurons[n].getIndex() ) 
				.append( ",\"potential\":" ) 
				.append( neurons[n].getPotential() ) 
				;
			}
			if( n < inputs.length ) {
				rc.append( ",\"fx\": 0 ") ;
			} else if ( n>=(neurons.length - outputs.length) ) {
				rc.append( ",\"fx\": 800 ") ;
			} else {
				int x = ( getLayerFromIndex(n) + 1 ) * layerWidth ;
				rc.append( ",\"fx\":").append( x ) ;				
			}
			
			if( shouldDrawThisOne ) {
				rc.append( " }" ) ;
			}
			
			sep = ',' ;
		}
	}

	private void printLinks( StringBuilder rc ) {
		char sep = ' '  ;
		if( !tooBigToPrint() ) {
			for( int n=0 ; n<neurons.length ; n++  ) {
				for( int e=0 ; e<edges[n].size() ; e++ ) {
					Edge edge = edges[n].get(e) ;
					Neuron source = neurons[edge.source()] ;
					Neuron target = neurons[n] ;
	
					rc.append( sep ) 
					.append( "{\"source\":" ) 		
					.append( source.getIndex() ) 
					.append( ",\"target\":" ) 
					.append( target.getIndex() ) 
					.append( ",\"weight\":" ) 
					.append( edge.weight() ) 		
					.append( " }" ) 
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


	public Genome toGenome() {
		Genome rc = new Genome() ; 
		rc.set( neurons.length, 0 ) ;
		rc.set( inputs.length, 1 ) ;
		rc.set( outputs.length, 2 ) ;
		rc.set( layerSizes.length, 3 ) ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			rc.set( layerSizes[i], 4+i ) ;
		}

		for( int i=0 ; i<neurons.length ; i++ ) {
			rc.append( neurons[i].toGenome() ) ;
		}
		for( int i=0 ; i<edges.length ; i++ ) {
			rc.append( edges[i].toGenome() ) ;
		}
		return rc ;
	}

	public Neuron getNeuron( int ix ) {
		return neurons[ix] ;
	}
	public EdgeList getEdgeList( int ix ) {
		return edges[ix] ;
	}
	public int getNumNeurons() {
		return neurons.length ;
	}
}


class Potentials {
	public double score ;
	public double clock ;
	public NeuronState states[] ;
	public double outputs[] ;
}

class NeuronState {
	public double potential ;
	public int id ;
	public int ago ;
	public NeuronState( Neuron n, int clk ) {
		this.potential = n.getPotential() ;
		this.id = n.getIndex() ;
		this.ago = clk - n.lastSpikeTime ;
	}
}

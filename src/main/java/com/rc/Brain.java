package com.rc ;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
	private final EdgeList targetEdges[] ;		// adjacency list ( directed & weighted )

	private final int layerSizes[] ;			// the size of each layer in the brain
	private final Queue<Integer> trainingPatternQueue ;

	/**
	 * Create a brain from a genome (bitmask).
	 * 
	 * @param g
	 */
	public Brain( Genome g ) {
		int numNeurons = 0 ;
		layerSizes = new int[ g.getInt(0) ] ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			layerSizes[i] = g.getInt( i+1 ) ;
			numNeurons += layerSizes[i] ; 
		}
		GENOME_SIZE = 1 + layerSizes.length ;
		
		neurons = new Neuron[ numNeurons ] ;
		int start = GENOME_SIZE ;
		for( int i=0 ; i<numNeurons ; i++ ) {
			Genome gs = g.subSequence(start, Neuron.GENOME_SIZE ) ;
			neurons[i] = new Neuron( gs ) ;
			start += Neuron.GENOME_SIZE ;
		}

		targetEdges = new EdgeList[ numNeurons ] ;

		for( int i=0 ; i<targetEdges.length ; i++ ) {
			int numEdges = g.getInt( start ) ;
			Genome gs = g.subSequence(start, numEdges*Edge.GENOME_SIZE + 1 ) ;
			targetEdges[i] = new EdgeList( gs ) ;
			start += numEdges*Edge.GENOME_SIZE + 1 ;
		}

		int numOutputs = layerSizes[ layerSizes.length - 1 ] ;
		this.outputHistory = new double[HISTORY_LENGTH][numOutputs] ;
		trainingPatternQueue = new LinkedList<>() ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			trainingPatternQueue.add( -1 ) ;
		}
	}

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
	 * @param numInputs 
	 * @param numOutputs
	 * @param dims the dimensions, up to 3 are relevant
	 */
	public Brain( int ... layers ) {
		int numOutputs = layers[ layers.length-1 ] ;
		this.outputHistory = new double[HISTORY_LENGTH][ numOutputs] ; 
		this.historyIndex = 0 ;
		
		int numNeurons = 0 ;
		this.layerSizes = new int[ layers.length ] ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			layerSizes[i] = layers[i] ;
			numNeurons += layers[i] ;
		}
		
		GENOME_SIZE = 1 + layerSizes.length ;

		this.neurons = new Neuron[ numNeurons ] ;
		for( int i = 0 ; i<layerSizes[0] ; i++ ) {
			neurons[i] = new InputNeuron( i ) ;
		}
		for( int i = layerSizes[0] ; i<numNeurons ; i++ ) {
			neurons[i] = new Neuron( i ) ;
		}

		// All neurons have edges 
		this.targetEdges = new EdgeList[ numNeurons ] ;
		for( int i=0 ; i<targetEdges.length ; i++ ) {
			this.targetEdges[i] = new EdgeList() ;
		}
		int edgeId = 100 ;
		

		// Now connect each layer to the previous layer
		for( int l=1 ; l<layerSizes.length ; l++ ) {
			for( int i=0 ; i<layerSizes[l] ; i++ ) {

				int tgtIndex = getIndexOfFirstInLayer(l)+i ; 
				EdgeList el = targetEdges[ tgtIndex ] ;
				
				for( int j=0 ; j<layerSizes[l-1] ; j++ ) {
					double weight = getRandomWeight() ;
					Edge e = new Edge( getIndexOfFirstInLayer(l-1)+j, tgtIndex, weight, edgeId++ ) ;
					if( Math.abs( i-j ) < 4 ) {
						el.add( e ) ;
					}
				}
			}
		}

		
		// Connect neighbours in each layer
		for( int l=0 ; l<layerSizes.length ; l++ ) {
			for( int i=0 ; i<layerSizes[l] ; i++ ) {	
				// Get edge list for each node in a layer
				int tgtIndex = getIndexOfFirstInLayer(l)+i ;
				EdgeList el = targetEdges[ tgtIndex ] ;
				if( i>0 ) {
					double weight = getRandomWeight() ;
					Edge e = new Edge( getIndexOfFirstInLayer(l)+i-1, tgtIndex, weight, edgeId++  ) ;
					//el.add( e ) ;
				}
				if( i<layerSizes[l]-1 ) {
					double weight = getRandomWeight() ;
					Edge e = new Edge( getIndexOfFirstInLayer(l)+i+1, tgtIndex, weight, edgeId++  ) ;
					//el.add( e ) ;
				}
			}
		}
		trainingPatternQueue = new LinkedList<>() ;
		for( int i=0 ; i<layerSizes.length ; i++ ) {
			trainingPatternQueue.add( -1 ) ;
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
	public void step( double[] inputs ) {
		
		// Set inputs immediately - no dependencies
		for( int i=0 ; i<layerSizes[0] ; i++ ) {
			this.neurons[i].setPotential( inputs[i] ) ;
		}

		// Will build up all outputs - before changing any of them
		double newPotentials[] = new double[ neurons.length ] ;

		// Sum all inputs 
		for( int n=inputs.length ; n<neurons.length; n++ ) {
			newPotentials[n] = 0 ;
			for( int e=0 ; e<targetEdges[n].size() ; e++ ) {
				Edge edge = targetEdges[n].get(e) ;
				newPotentials[n] += neurons[edge.source()].getPotential() * edge.weight() ;
			}
		}

		// Then write the output as an atomic op
		for( int n=inputs.length ; n<neurons.length; n++ ) {
			neurons[n].setPotential( newPotentials[n] ) ;
		}
		
	}

	public void train( int patternIndex ) {
		int pi = trainingPatternQueue.remove() ;
		if( pi >= 0 ) {
			Neuron n = getNeuron( layerSizes.length-1, pi ) ;
		//	n.setPotential( 1 ) ;
		}
		
		/*
		for( int l=0 ; l<layerSizes.length ; l++ ) {
			int spikingIndex = -1 ;
			for( int i=0 ; i<layerSizes[l] ; i++ ) {	
				int tgtIndex = getIndexOfFirstInLayer(l)+i ;
				Neuron n = getNeuron( tgtIndex ) ;
				
				if( n.isSpiking() ) {
					if( spikingIndex<0 ) {
						spikingIndex = i ;
					} else {
						EdgeList el = getIncomingEdges(tgtIndex) ;
						for( Edge e : el ) {
							e.addWeight( -n.learningRate / 4 ) ;
						}
 					}
				}
			}
		}
		*/
		
		trainingPatternQueue.add( patternIndex ) ;
		for( int i=0 ; i<neurons.length; i++ ) {
			neurons[i].train( this ) ;
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


	public Potentials getNeuronPotentials( int patternIndex, int clk ) {
		Potentials rc = new Potentials() ;
		int outputLayer = layerSizes.length - 1 ;
		
		rc.outputs = new double[ layerSizes[outputLayer] ] ;
		for( int i=0 ; i<rc.outputs.length ; i++ ) {
			
			rc.outputs[i] = getNeuron( outputLayer, i ).getPotential() ;
		}

		rc.neurons = new NeuronState[ neurons.length ] ;
		for( int i=0 ; i<neurons.length ; i++ ) {
			rc.neurons[i] = new NeuronState( neurons[i], clk ) ;
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
		int layerWidth = 800 / ( layerSizes.length - 1 );
		int numOutputs = layerSizes[ layerSizes.length-1 ] ;
		int numInputs = layerSizes[0 ] ;
		for( int n=0 ; n<neurons.length ; n++ ) {
			
			boolean shouldDrawThisOne = !tooBigToPrint() || n < numInputs || n>=(neurons.length - numOutputs) ;

			if( shouldDrawThisOne ) {
				int x = ( getLayerFromIndex(n) ) * layerWidth ;

				rc.append( sep ) 
				.append( "{ \"id\":"  ) 
				.append( neurons[n].getIndex() ) 
				.append( ",\"potential\":" ) 
				.append( neurons[n].getPotential() ) 
				.append( ",\"fx\":").append( x ) 				
				.append( " }" ) ;
			}
			
			sep = ',' ;
		}
	}

	private void printLinks( StringBuilder rc ) {
		char sep = ' '  ;
		if( !tooBigToPrint() ) {
			for( int n=0 ; n<neurons.length ; n++  ) {
				for( int e=0 ; e<targetEdges[n].size() ; e++ ) {
					Edge edge = targetEdges[n].get(e) ;
					Neuron source = neurons[edge.source()] ;
					Neuron target = neurons[n] ;
	
					rc.append( sep ) 
					.append( "{\"source\":" ) 		
					.append( source.getIndex() ) 
					.append( ",\"target\":" ) 
					.append( target.getIndex() ) 
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



	public Neuron getNeuron( int ix ) {
		return neurons[ix] ;
	}
	public Neuron getNeuron( int layer, int ix ) {
		int n = 0 ;
		for( int i=0 ; i<layer ; i++ ) {
			n += layerSizes[i] ;
		}
		return getNeuron( n+ix ) ;
	}

	public int getNumNeurons() {
		return neurons.length ;
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
}


class Potentials {
	public double score ;
	public double clock ;
	public NeuronState neurons[] ;
	public EdgeState edges[] ;
	public double outputs[] ;
}

class NeuronState {
	public double potential ;
	public int id ;
	public int ago ;
	public NeuronState( Neuron n, int clk ) {
		this.potential = n.getPotential() ;
		this.id = n.getIndex() ;
		this.ago = n.spikeIndex ;
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

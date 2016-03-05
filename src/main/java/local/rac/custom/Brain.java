package local.rac.custom;

import java.util.Iterator;

import scala.util.Random;

public class Brain implements Iterable<Neuron>{

	final public static double STANDARD = -1.5 ;			// reasonable value for network connections 
	final public static double FULL = 0 ;					// all neurons connected to each other

	final public static int HISTORY_LENGTH = 100 ;
	
	final private EvolutionRules evolutionRules ;			// How to train the network

	private double desiredOutputHistory[][] ;				// in train mode - this is the target state
	private int clock ;								

	private Neuron[] neurons ;								// list of neurons in the brain 

	private int [] brainDimensions ;						// the grid size(s) of the brain ( e.g. a 3x3 brain has 3 layes of 3 neurons )
	private InputNeuron[] inputs ;							// which nodes are .. inputs 
	private OutputNeuron [] outputs ;						// .. and outputs

	private final Random rng ;								// utility randon number generator

	public Brain( double connectivityFactor, int inputCount, int outputCount, int [] brainDimensions ) {
		this.rng = new Random( 100 ) ;
		this.evolutionRules = new EvolutionRules() ;

		this.brainDimensions = brainDimensions ;

		int numNeurons = 1 ;
		for( int i=0 ; i<brainDimensions.length ; i++ ) {
			numNeurons *= brainDimensions[i] ; 
		}
		neurons = new Neuron[numNeurons] ;

		if( numNeurons < ( inputCount+outputCount) ) {
			throw new RuntimeException( "The total neuron count must be more than the sum of inputs and outputs." ) ;
		}

		for( int neuronIndex = 0 ; neuronIndex<numNeurons ; neuronIndex++ ) {
			neurons[neuronIndex] = new Neuron( this, neuronIndex ) ;
		}

		inputs = new InputNeuron[inputCount] ;
		outputs = new OutputNeuron[outputCount] ;

		for( int i=0 ; i<inputs.length ; i++ ) {
			int neuronIndex = i ;
			inputs[i] = new InputNeuron( this, neuronIndex ) ;
			neurons[neuronIndex] = inputs[i] ; 			
		}
		for( int i=0 ; i<outputs.length ; i++ ) {
			int neuronIndex = neurons.length-i-1 ;		
			outputs[i] = new OutputNeuron( this, neuronIndex ) ;
			neurons[neuronIndex] = outputs[i] ; 			
		}		

		for( int targetNeuronIndex = 0 ; targetNeuronIndex<numNeurons ; targetNeuronIndex++ ) {
			Neuron target = neurons[targetNeuronIndex] ;
			int []targetLocation = getLocationFromIndex( targetNeuronIndex ) ;

			for( int inputNeuronIndex = 0 ; inputNeuronIndex<numNeurons ; inputNeuronIndex++ ) {
				if( inputNeuronIndex != targetNeuronIndex ) {
					Neuron input = neurons[inputNeuronIndex] ;
					if( input.getType() != NeuronType.OUTPUT ) {
						int []sourceLocation = getLocationFromIndex( inputNeuronIndex ) ;

						double distance =  0.0 ;
						for( int i=0 ; i<targetLocation.length ; i++ ) {
							distance += ( targetLocation[i] - sourceLocation[i] ) * ( targetLocation[i] - sourceLocation[i] ) ;
						}
						distance = Math.sqrt( distance ) ;
						double chanceOfConnection = Math.exp( distance * connectivityFactor ) ;

						if( rng.nextDouble() < chanceOfConnection && !( target.getType()==NeuronType.OUTPUT && input.getType()==NeuronType.INPUT ) ) {
							Axon axon = new Axon( input, rng.nextGaussian() ) ;							
							target.addInput( axon ) ;
						}
					}
				}
			}
		}		


		desiredOutputHistory = new double[ HISTORY_LENGTH ][] ;
		for( int i=0 ; i<desiredOutputHistory.length ; i++ ) {
			desiredOutputHistory[i] = new double[ outputs.length ] ;
		}
		this.clock = 0 ;
	}

	public double train( double[] inputs, double[] outputs ) {

		for( int i=0 ; i<outputs.length ; i++ ) {
			desiredOutputHistory[clock][i] = outputs[i] ;			
		}

		clock( inputs ) ;

		double []outputError = new double[ outputs.length ] ;		
		for( int i=0 ; i<desiredOutputHistory.length ; i++ ) {
			for( int j=0 ; j<outputs.length ; j++ ) {
				outputError[j] += desiredOutputHistory[i][j] - this.outputs[j].getPotential(i) ;  
			}
		}

		boolean [] visitedNeuronIndex = new boolean[neurons.length] ;

		for( int i=0 ; i<outputs.length ; i++ ) {
			evolutionRules.evolve( outputError[i], this.outputs[i], visitedNeuronIndex ) ;
		}
		
		double rc = 0.0 ;
		for( double d : outputError ) {
			rc += d * d ;
		}
		return Math.sqrt( rc ) ;
	}


	public void clock( double[] inputs ) {
		for( int i=0 ; i<inputs.length ; i++ ) {
			this.inputs[i].setPotential( inputs[i] );
		}
		clock() ;
	}


	public void clock()  {
		boolean [] visitedNeuronIndex = new boolean[neurons.length] ;

		for( int i=0 ; i<outputs.length ; i++ ) {
			clock( outputs[i], visitedNeuronIndex ) ;
		}	
		
		clock++ ;
		if( clock >= HISTORY_LENGTH ) clock = 0 ;
	}

	public void clock( Neuron n, boolean [] visitedNeuronIndex )  {
		if( visitedNeuronIndex[n.getIndexInBrain()] ) {
			return ;
		}
		visitedNeuronIndex[n.getIndexInBrain()] = true ;

		for( Axon axon : n ) {
			clock( axon.getNeuron(), visitedNeuronIndex ) ;
		}
		n.clock() ;		
	}

	public int[] getLocationFromIndex( int index ) {
		int [] rc = new int[ brainDimensions.length ] ;

		for( int dimIndex=0 ; dimIndex<brainDimensions.length ; dimIndex++ ) {
			rc[dimIndex] = index % brainDimensions[dimIndex] ;
			index /= brainDimensions[dimIndex] ;
		}

		return rc ;
	}

	public int getIndexFromLocation( int [] location ) {
		int rc = 0 ;

		for( int dimIndex=brainDimensions.length ; dimIndex>0; dimIndex-- ) {
			rc *= brainDimensions[dimIndex-1] ;
			rc += location[dimIndex-1] ;
		}

		return rc ;
	}


	public CharSequence getNeuronPotentials() {
		StringBuilder rc  = new StringBuilder( "{ \"potentials\": [" ) ;

		char sep = ' '  ;
		for( Neuron n : neurons ) {
			rc.append( sep ).append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			sep = ',' ;
		}
//		if( (clock%5)==0 ) {
			rc.append( "], \"historyIndex\": " ).append( clock ) ;
			rc.append( ", \"history\": [" ) ;
			sep = ' ' ;
			for( int i=0 ; i<desiredOutputHistory.length ; i++ ) {
				rc.append( sep ).append( '[' );
				char sep2 = ' ' ;
				for( int j=0 ; j<outputs.length ; j++ ) {
					rc.append( sep2 ).append( outputs[j].getPotential(i) ) ;
					sep2 = ',' ;
				}
				rc.append( ']' ) ;
				sep = ',' ;
			}
//		}
		return rc.append( "] }" ) ;
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

	private void printNodes( StringBuilder rc ) {
		char sep = ' '  ;
		int nodeIndex=0 ;
		for( Neuron n : neurons ) {
			rc.append( sep ) ;
			sep = ',' ;
			rc.append( "{\"name\":\"" ) ;
			int []loc = getLocationFromIndex(nodeIndex) ;

			for( int i=0 ; i<loc.length ; i++ ) 
				rc.append( loc[i] ).append( ' ' ) ;

			rc.append( "\",\"potential\":" ) ;
			rc.append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			rc.append( ",\"type\":\"" ) ;
			rc.append( n.getType().toString() ) ;				
			rc.append( "\" }" ) ;

			nodeIndex++ ;
		}
	}

	private void printLinks( StringBuilder rc ) {
		char sep = ' '  ;
		for( Neuron n : neurons ) {
			for( Axon in : n ) {
				rc.append( sep ) ;
				sep = ',' ;
				rc.append( "{\"source\":" ) ;				
				rc.append( in.getNeuron().getIndexInBrain() ) ;
				rc.append( ",\"target\":" ) ;
				rc.append( n.getIndexInBrain() ) ;
				rc.append( ",\"weight\":" ) ;
				rc.append( in.getMembraneTransmissionFactor() ) ;				

				rc.append( " }" ) ;
			}
		}
	}

	@Override
	public Iterator<Neuron> iterator() {
		return new Iterator<Neuron>() {
			private int p=0;

			public boolean hasNext() {
				return neurons.length>p;
			}

			public Neuron next() {
				return neurons[p++];
			}

			public void remove() {
				throw new UnsupportedOperationException("Cannot remove an element of an array.");
			}
		} ;
	}

	public int getClock() {
		return clock;
	}
}



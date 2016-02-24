package local.rac.custom;

import java.util.Iterator;

import scala.util.Random;

public class Brain implements Iterable<Neuron>{

	final public static double STANDARD = -1.5 ;
	final public static double FULL = 0 ;

	final private EvolutionRules evolutionRules ;

	private double outputHistory[][] ;
	private int historyIndex ;

	private Neuron[] neurons ;
	private int [] brainDimensions ;
	private int [] inputIndices ;
	private int [] outputIndices ;

	private final Random rng ;

	public Brain( double inhibitorProbability, double connectivityFactor, int inputCount, int outputCount, int [] brainDimensions ) {
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
			neurons[neuronIndex] = new Neuron( neuronIndex ) ;
		}

		for( int targetNeuronIndex = 0 ; targetNeuronIndex<numNeurons ; targetNeuronIndex++ ) {
			Neuron target = neurons[targetNeuronIndex] ;
			int []targetLocation = getLocationFromIndex( targetNeuronIndex ) ;

			for( int inputNeuronIndex = 0 ; inputNeuronIndex<numNeurons ; inputNeuronIndex++ ) {
				if( inputNeuronIndex != targetNeuronIndex ) {
					int []sourceLocation = getLocationFromIndex( inputNeuronIndex ) ;

					double distance =  0.0 ;
					for( int i=0 ; i<targetLocation.length ; i++ ) {
						distance += ( targetLocation[i] - sourceLocation[i] ) * ( targetLocation[i] - sourceLocation[i] ) ;
					}
					distance = Math.sqrt( distance ) ;
					double chanceOfConnection = Math.exp( distance * connectivityFactor ) ;

					if( rng.nextDouble() < chanceOfConnection ) {
						Neuron input = neurons[inputNeuronIndex] ;
						if( rng.nextDouble() < inhibitorProbability ) {
							target.connectInhibitor(input, rng.nextDouble() );
						} else {
							target.connectExcitor(input, rng.nextDouble());
						}
					}
				}
			}
		}		

		inputIndices = new int[inputCount] ;
		outputIndices = new int[outputCount] ;

		for( int i=0 ; i<inputIndices.length ; i++ ) {
			inputIndices[i] = i ;
			neurons[inputIndices[i]].setType( NeuronType.INPUT ) ;
		}
		for( int i=0 ; i<outputIndices.length ; i++ ) {
			outputIndices[i] = neurons.length-i-1 ;		
			neurons[outputIndices[i]].setType( NeuronType.OUTPUT ) ;
		}		

		outputHistory = new double[ 100 ][] ;
		for( int i=0 ; i<outputHistory.length ; i++ ) {
			outputHistory[i] = new double[ outputIndices.length ] ;
		}
		this.historyIndex = 0 ;
	}

	public double train( double[] inputs, double[] outputs ) {
		for( int i=0 ; i<inputs.length ; i++ ) {
			neurons[this.inputIndices[i]].setPotential( inputs[i] );
		}
		clock() ;

		boolean [] visitedNeuronIndex = new boolean[neurons.length] ;

		double [] outputError = new double[ outputs.length ] ;
		for( int i=0 ; i<outputs.length ; i++ ) {
			outputError[i] = outputs[i] - neurons[ this.outputIndices[i] ].getPotential() ;

			Neuron n = neurons[ this.outputIndices[i] ] ;
			evolutionRules.evolve( outputError[i], n, visitedNeuronIndex ) ;
		}
		
		double rc = 0.0 ;
		for( int i=0 ; i<outputError.length ; i++ ) {
			rc += outputError[i] * outputError[i] ;
		}
		return Math.sqrt( rc ) ;
	}

	public void clock( double[] inputs ) {
		for( int i=0 ; i<inputs.length ; i++ ) {
			neurons[this.inputIndices[i]].setPotential( inputs[i] );
		}
		clock() ;
	}

	public void clock()  {
		for( Neuron n : neurons ) {
			n.clock() ;
		}
		for( int i=0 ; i<this.outputIndices.length ; i++ ) {
			outputHistory[historyIndex][i] = neurons[this.outputIndices[i]].getPotential() ; 
		}
		historyIndex++ ;
		if( historyIndex >= outputHistory.length ) {
			historyIndex=0 ;
		}
	}

	public double[][] getOutputHistory() {
		return outputHistory ;
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
		if( (historyIndex%5)==0 ) {
			rc.append( "], \"historyIndex\": " ).append( historyIndex ) ;
			rc.append( ", \"history\": [" ) ;
			sep = ' ' ;
			for( int i=0 ; i<outputHistory.length ; i++ ) {
				rc.append( sep ).append( '[' );
				char sep2 = ' ' ;
				for( int j=0 ; j<outputHistory[i].length ; j++ ) {
					rc.append( sep2 ).append( outputHistory[i][j] ) ;
					sep2 = ',' ;
				}
				rc.append( ']' ) ;
				sep = ',' ;
			}
		}
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
			for( NeuronWeight in : n ) {
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

}



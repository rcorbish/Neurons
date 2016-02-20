package local.rac.custom;

import scala.util.Random;

public class Brain {

	final public static double STANDARD = -1 ;
	final public static double FULL = 0 ;

	private Neuron[] neurons ;
	private int [] brainDimensions ;
	private int [] inputs ;
	private int [] outputs ;

	private final Random rng ;

	public Brain( double inhibitorProbability, double connectivityFactor, int inputCount, int outputCount, int [] brainDimensions ) {
		this.rng = new Random( 100 ) ;

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

		inputs = new int[inputCount] ;
		outputs = new int[outputCount] ;

		for( int i=0 ; i<inputs.length ; i++ ) {
			inputs[i] = i ;
			neurons[inputs[i]].setType( NeuronType.INPUT ) ;
		}
		for( int i=0 ; i<outputs.length ; i++ ) {
			outputs[i] = neurons.length-i-1 ;		
			neurons[outputs[i]].setType( NeuronType.OUTPUT ) ;
		}		
	}


	public void clock( double[] inputs ) {
		for( int i=0 ; i<inputs.length ; i++ ) {
			neurons[this.inputs[i]].setPotential( inputs[i] );
		}
		clock() ;
	}

	public void clock()  {
		for( Neuron n : neurons ) {
			n.clock() ;
		}
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
		StringBuilder rc  = new StringBuilder( "[" ) ;

		char sep = ' '  ;
		for( Neuron n : neurons ) {
			rc.append( sep ) ;
			sep = ',' ;
			rc.append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
		}
		return rc.append( ']' ) ;
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

}



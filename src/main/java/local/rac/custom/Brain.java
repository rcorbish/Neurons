package local.rac.custom;

import java.util.Iterator;
import java.util.Stack;

import scala.util.Random;

public class Brain implements Iterable<Neuron>{

	final public static double STANDARD = -0.75 ;			// reasonable value for network connections 
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

		for( OutputNeuron on : outputs ) {
			setOutputPaths( on ) ;
		}

		desiredOutputHistory = new double[ HISTORY_LENGTH ][] ;
		for( int i=0 ; i<desiredOutputHistory.length ; i++ ) {
			desiredOutputHistory[i] = new double[ outputs.length ] ;
		}
		this.clock = 0 ;
	}

	public double train( double[] inputs, double[] desiredOutput ) {

		for( int i=0 ; i<outputs.length ; i++ ) {
			desiredOutputHistory[clock][i] = desiredOutput[i] ;			
		}

		updateNeuronOutputs( inputs ) ;
		double currentError = calculateErrors() ;

		double proposedError = 0.0 ;
		Neuron randomNeuron = null ;
		double axonTransmissionAdjustment = 0.0 ;
		int maxChanges = 15 ;
		while( proposedError < currentError && --maxChanges>0 ) {

			int randomNeuronIndex = rng.nextInt( neurons.length ) ;
			randomNeuron = neurons[ randomNeuronIndex ] ;

			axonTransmissionAdjustment = rng.nextGaussian() * 1e-6 ;
			for( Axon axon : randomNeuron ) {
				axon.adjustMembraneTransmissionFactor(axonTransmissionAdjustment);
			}

			updateNeuronOutputs() ;

			proposedError = currentError ;
			currentError = calculateErrors() ;
		}
		if(randomNeuron != null ) {
			for( Axon axon : randomNeuron ) {
				axon.adjustMembraneTransmissionFactor(-axonTransmissionAdjustment);
			}
		}
		clock = getNextClock() ;
		return currentError ;
	}


	protected double calculateErrors() {

		double []errors = new double[ outputs.length ] ;		
		for( int i=0 ; i<HISTORY_LENGTH ; i++ ) {
			for( int j=0 ; j<outputs.length ; j++ ) {
				int distanceFromNow = Math.abs( j - clock ) ;
				errors[j] += ( desiredOutputHistory[i][j] - outputs[j].getPotential(i) ) / ( 1 + distanceFromNow );  
			}
		}

		double rc = 0.0 ;
		for( double d : errors ) {
			rc += d * d ;
		}

		return rc ;
	}

	public void updateNeuronOutputs( double[] inputs ) {
		for( int i=0 ; i<inputs.length ; i++ ) {
			this.inputs[i].setPotential( inputs[i] );
		}
		updateNeuronOutputs() ;
	}

	public void updateNeuronOutputs()  {
		for( OutputNeuron o : outputs ) {
			o.visitPathwayToOutput( (Neuron n)-> n.updatePotential() ) ;
		}
	}


	protected void setOutputPaths( OutputNeuron outputNeuron ) {
		Stack<Neuron> path = new Stack<>() ; 
		setOutputPaths(outputNeuron, path );
		int [] indexPath = new int[ path.size() ] ;
		for( int i=0 ; i<indexPath.length ; i++ ) {
			indexPath[i] = path.pop().getIndexInBrain() ;
		}
		outputNeuron.setBrainPathway( indexPath ) ;
	}

	protected void setOutputPaths( Neuron n, Stack<Neuron> path ) {

		path.push( n ) ;
		for( Axon axon : n ) {
			if( !path.contains( axon.getNeuron() ) ) {
				setOutputPaths( axon.getNeuron(), path ) ;
			}
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

	public Neuron getNeuronByIndex( int ix ) { return neurons[ix] ; }

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
		for( int i=0 ; i<HISTORY_LENGTH ; i++ ) {
			rc.append( sep ).append( '[' );
			char sep2 = ' ' ;
			for( int j=0 ; j<outputs.length ; j++ ) {
				rc.append( sep2 ).append( outputs[j].getPotential(i) ) ;
				sep2 = ',' ;
			}
			rc.append( ']' ) ;
			sep = ',' ;
		}
		rc.append( "], \"target\": [" ) ;
		sep = ' ' ;
		for( int i=0 ; i<HISTORY_LENGTH ; i++ ) {
			rc.append( sep ).append( '[' );
			char sep2 = ' ' ;
			for( int j=0 ; j<outputs.length ; j++ ) {
				rc.append( sep2 ).append( desiredOutputHistory[i][j] ) ;
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
	public int getNextClock() {
		return ( clock < (HISTORY_LENGTH-1) ) ? clock + 1 : 0 ;
	}
}



package com.rc ;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.eclipse.jetty.util.ArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Brain implements Iterable<Neuron>{

	final static Logger log = LoggerFactory.getLogger( Monitor.class ) ;
	
	final public static double STANDARD = 0.75 ;			// reasonable value for network connections 
	final public static double FULL = 0 ;					// all neurons connected to each other

	final public static double INHIBITOR_RATIO = 0.2 ;
		
	private Neuron[] neurons ;								// list of neurons in the brain 

	private int [] brainDimensions ;						// the grid size(s) of the brain ( e.g. a 3x3 brain has 3 layes of 3 neurons )
	private InputNeuron  [] inputs ;						// which nodes are .. inputs 
	private OutputNeuron [] outputs ;						// .. and outputs

	private final Random rng ;								// utility randon number generator

	public Brain( double connectivityFactor, int inputCount, int outputCount, int ... brainDimensions ) {
		this.rng = new Random( 20 ) ;

		this.brainDimensions = brainDimensions ;

		int numNeurons = 1 ;
		for( int i=0 ; i<brainDimensions.length ; i++ ) {
			numNeurons *= brainDimensions[i] ; 
		}
		List<Neuron> neurons = new ArrayList<>() ;

		if( numNeurons < ( inputCount+outputCount) ) {
			throw new RuntimeException( "The total neuron count must be more than the sum of inputs and outputs." ) ;
		}

		for( int neuronIndex = 0 ; neuronIndex<numNeurons ; neuronIndex++ ) {
			neurons.add( new Neuron( this, neuronIndex ) ) ;
		}

		log.info( "Created {} neurons", neurons.size() ) ;

		for( int targetNeuronIndex = 0 ; targetNeuronIndex<numNeurons ; targetNeuronIndex++ ) {
			Neuron target = neurons.get( targetNeuronIndex ) ;

			for( int inputNeuronIndex = 0 ; inputNeuronIndex<numNeurons ; inputNeuronIndex++ ) {
				Neuron input = neurons.get( inputNeuronIndex ) ;

				double distance =  distance(targetNeuronIndex,inputNeuronIndex) ;
				double chanceOfConnection = Math.exp( (1+distance) * -connectivityFactor ) ;

				if( rng.nextDouble() < chanceOfConnection ) { 
					double weight = ( rng.nextDouble() > INHIBITOR_RATIO ) ?
							distance*rng.nextDouble() : -rng.nextDouble() ;

					Axon axon = new Axon( input, weight ) ;							
					target.addInput( axon ) ;
				}
			}
		}		
		inputs = new InputNeuron[inputCount] ;
		outputs = new OutputNeuron[outputCount] ;

		for( int i=0 ; i<inputs.length ; i++ ) {
			inputs[i] = new InputNeuron( this, i ) ;
			for( Neuron n : neurons ) {
				int ix  = n.getIndexInBrain() ;
				int loc[] = getLocationFromIndex(ix) ;
				if( loc[0] == 0 ) {
					double weight = 
					( rng.nextDouble() > INHIBITOR_RATIO ) ?
					rng.nextDouble() : -rng.nextDouble() ;

					Axon axon = new Axon( inputs[i], weight ) ;
					n.addInput(axon);
				}
			}
		}

		
		for( int i=0 ; i<outputs.length ; i++ ) {
			outputs[i] = new OutputNeuron( this, i ) ;
			for( Neuron n : neurons ) {
				int ix  = n.getIndexInBrain() ;
				int loc[] = getLocationFromIndex(ix) ;
				if( loc[0] == brainDimensions[0]-1 ) {
					double weight = 
					( rng.nextDouble() > INHIBITOR_RATIO ) ?
					rng.nextDouble() : -rng.nextDouble() ;

					Axon axon = new Axon( n, weight ) ;
					outputs[i].addInput(axon);
				}
			}
		}
		while( removeDeadReferences( neurons ) ) {} ;

		this.neurons = new Neuron[ neurons.size() ] ;
		neurons.toArray( this.neurons ) ;
		log.info( "Copied {} neurons", this.neurons.length ) ;
	}

	public void step( double[] inputs ) {
		for( int i=0 ; i<inputs.length ; i++ ) {
			this.inputs[i].setPotential( inputs[i] );
		}
		for( Neuron n : this ) {
			n.updatePotential() ;
		}
		for( Neuron n : outputs ) {
			n.updatePotential() ;
		}
		for( Neuron n : this ) {
			n.clock() ;
		}
		for( Neuron n : outputs ) {
			n.clock() ;
		}
	}

	protected boolean removeDeadReferences( List<Neuron> neurons ) {
		Set<Neuron> visited = new HashSet<>() ; 
		Queue<Neuron> queue = new ArrayQueue<>() ;
		for( Neuron output : outputs ) {
			queue.add( output ) ;
		}
		while( !queue.isEmpty() ) {
			Neuron n = queue.remove() ; 
			if( visited.add(n) ) {
				log.info( "Processing {}", n.getName() ) ;
				for( Axon a : n ) {
					Neuron n2 = a.getNeuron() ;
					queue.add( n2 ) ;
				}
			}
		}
		boolean removed = false ;
		log.info( "Visited {} neurons", visited.size() ) ;
		for( Iterator<Neuron> i=neurons.iterator() ; i.hasNext() ; ) {
			Neuron dead = i.next() ;
			if( !visited.contains( dead ) ) {
				i.remove() ;
				removed = true ;
			}
		}
		return removed ;
	}


	/**
	 * Determine the coordinates of a neuron in the brain
	 */
	public int[] getLocationFromIndex( int index ) {
		int [] rc = new int[ brainDimensions.length ] ;

		for( int dimIndex=0 ; dimIndex<brainDimensions.length ; dimIndex++ ) {
			rc[dimIndex] = index % brainDimensions[dimIndex] ;
			index /= brainDimensions[dimIndex] ;
		}

		return rc ;
	}

	/**
	 * Given an index - which neuron is it
	 */
	public int getIndexFromLocation( int [] location ) {
		int rc = 0 ;

		for( int dimIndex=brainDimensions.length ; dimIndex>0; dimIndex-- ) {
			rc *= brainDimensions[dimIndex-1] ;
			rc += location[dimIndex-1] ;
		}

		return rc ;
	}

	/**
	 * @see #getIndexFromLocation(int[])
	 */
	public Neuron getNeuronByIndex( int ix ) { return neurons[ix] ; }

	/**
	 * Calculate Euclidian distance between two neurons
	 */
	public double distance( int n0, int n1 ) {
		int l0[] = getLocationFromIndex( n0 ) ;
		int l1[] = getLocationFromIndex( n1 ) ;
		
		double distance =  0.0 ;
		for( int i=0 ; i<l0.length ; i++ ) {
			distance += ( l0[i] - l1[i] ) * ( l0[i] - l1[i] ) ;
		}
		return Math.sqrt( distance ) ;
	}

	public InputNeuron[] getInputs() { return inputs ; }
	public OutputNeuron[] getOutputs() { return outputs ; }

	public Potentials getNeuronPotentials() {
		Potentials rc = new Potentials() ;
		rc.inputs = new double[ inputs.length ] ;
		for( int i=0 ; i<rc.inputs.length ; i++ ) {
			rc.inputs[i] = inputs[i].getPotential() ;
		}
		rc.outputs = new double[ outputs.length ] ;
		for( int i=0 ; i<rc.outputs.length ; i++ ) {
			rc.outputs[i] = outputs[i].getPotential() ;
		}
		rc.potentials = new double[ neurons.length ] ;
		for( int i=0 ; i<rc.potentials.length ; i++ ) {
			rc.potentials[i] = neurons[i].getPotential() ;
		}
		return rc ;
	}

	public CharSequence getNeuronPotentials2() {
		StringBuilder rc  = new StringBuilder( "{ \"potentials\": [" ) ;

		char sep = ' '  ;
		for( Neuron n : neurons ) {
			rc.append( sep ).append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			sep = ',' ;
		}
		rc.append( "], \"inputs\": [" ) ;
		sep = ' '  ;
		for( Neuron n : inputs ) {
			rc.append( sep ).append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			sep = ',' ;
		}
		rc.append( "], \"outputs\": [" ) ;
		sep = ' '  ;
		for( Neuron n : outputs ) {
			rc.append( sep ).append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			sep = ',' ;
		}
		rc.append( "] }" ) ;
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

	private void printNodes( StringBuilder rc ) {
		char sep = ' '  ;
		for( Neuron n : inputs ) {
			rc.append( sep ) ;
			sep = ',' ;
			rc.append( "{\"name\":\"" ) ;
			rc.append( n.getName() ) ;
			rc.append( "\",\"potential\":" ) ;
			rc.append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			rc.append( ",\"type\":\"" ) ;
			rc.append( n.getType().toString() ) ;				
			rc.append( "\",\"fx\":10" ) ;
			rc.append( " }" ) ;
		}
		for( Neuron n : outputs ) {
			rc.append( sep ) ;
			sep = ',' ;
			rc.append( "{\"name\":\"" ) ;
			rc.append( n.getName() ) ;
			rc.append( "\",\"potential\":" ) ;
			rc.append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			rc.append( ",\"type\":\"" ) ;
			rc.append( n.getType().toString() ) ;				
			rc.append( "\",\"fx\":1100" ) ;
			rc.append( " }" ) ;
		}
		for( Neuron n : neurons ) {
			rc.append( sep ) ;
			sep = ',' ;
			rc.append( "{\"name\":\"" ) ;
			rc.append( n.getName() ) ;
			rc.append( "\",\"potential\":" ) ;
			rc.append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			rc.append( ",\"type\":\"" ) ;
			rc.append( n.getType().toString() ) ;				
			rc.append( "\" }" ) ;
		}
	}

	private void printLinks( StringBuilder rc ) {
		char sep = ' '  ;
		for( Neuron n : neurons ) {
			for( Axon in : n ) {
				rc.append( sep ) ;
				sep = ',' ;
				rc.append( "{\"source\":\"" ) ;		
				Neuron source = in.getNeuron() ;
				rc.append( source.getName() ) ;
				rc.append( "\",\"target\":\"" ) ;
				rc.append( n.getName() ) ;
				rc.append( "\",\"weight\":" ) ;
				rc.append( in.getMembraneTransmissionFactor() ) ;				
				rc.append( ",\"name\":\"" ) ;
				rc.append( source.getName() ) ;				
				rc.append( "-" ) ;
				rc.append( n.getName() ) ;
				rc.append( "\" }" ) ;
			}
		}
		for( Neuron n : outputs ) {
			for( Axon in : n ) {
				rc.append( sep ) ;
				sep = ',' ;
				rc.append( "{\"source\":\"" ) ;		
				Neuron source = in.getNeuron() ;
				rc.append( source.getName() ) ;
				rc.append( "\",\"target\":\"" ) ;
				rc.append( n.getName() ) ;
				rc.append( "\",\"weight\":" ) ;
				rc.append( in.getMembraneTransmissionFactor() ) ;				
				rc.append( ",\"name\":\"" ) ;
				rc.append( source.getName() ) ;				
				rc.append( "-" ) ;
				rc.append( n.getName() ) ;
				rc.append( "\" }" ) ;
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


class Potentials {
	public double potentials[] ;
	public double inputs[] ;
	public double outputs[] ;
}
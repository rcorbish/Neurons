package com.rc ;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class Brain implements Iterable<Neuron>{

	final static Logger log = LoggerFactory.getLogger( Monitor.class ) ;
		
	final static public int HISTORY_LENGTH = 100 ;

	private Neuron[] neurons ;								// list of neurons in the brain 

	private int [] brainDimensions ;						// the grid size(s) of the brain ( e.g. a 3x3 brain has 3 layes of 3 neurons )
	private InputNeuron  [] inputs ;						// which nodes are .. inputs 
	private OutputNeuron [] outputs ;						// .. and outputs

	private final Random rng ;								// utility random number generator
	private final BrainParameters parameters ;

	private final double scoreReservoir[] ;
	private int scoreClock ;
	
	public Brain( BrainParameters parameters ) {
		this.parameters = parameters ;
		this.rng = new Random( 24 ) ;
		this.scoreReservoir = new double[1000] ;
		this.scoreClock = 0 ;
		
		this.brainDimensions = parameters.dimensions ;

		int numNeurons = 1 ;
		for( int i=0 ; i<brainDimensions.length ; i++ ) {
			numNeurons *= brainDimensions[i] ; 
		}
		List<Neuron> neurons = new ArrayList<>() ;

		if( numNeurons < ( parameters.numInputs+parameters.numOutputs) ) {
			throw new RuntimeException( "The total neuron count must be more than the sum of inputs and outputs." ) ;
		}

		for( int neuronIndex = 0 ; neuronIndex<numNeurons ; neuronIndex++ ) {
			neurons.add( new Neuron( this, neuronIndex, parameters ) ) ;
		}

		log.debug( "Created {} neurons", neurons.size() ) ;

		for( int targetNeuronIndex = 0 ; targetNeuronIndex<numNeurons ; targetNeuronIndex++ ) {
			Neuron target = neurons.get( targetNeuronIndex ) ;

			for( int inputNeuronIndex = 0 ; inputNeuronIndex<numNeurons ; inputNeuronIndex++ ) {
				Neuron input = neurons.get( inputNeuronIndex ) ;

				double distance =  1 + distance(targetNeuronIndex,inputNeuronIndex) ;
				double chanceOfConnection = Math.exp( distance * -parameters.connectivityFactor ) ;

				if( rng.nextDouble() < chanceOfConnection ) { 
					double weight = ( rng.nextDouble() > parameters.inhibitorRatio ) ?
							distance*rng.nextDouble() : -rng.nextDouble() ;

					Axon axon = new Axon( input, weight ) ;							
					target.addInput( axon ) ;
				}
			}
		}		
		inputs = new InputNeuron[parameters.numInputs] ;
		outputs = new OutputNeuron[parameters.numOutputs] ;

		for( int i=0 ; i<inputs.length ; i++ ) {
			inputs[i] = new InputNeuron( this, i, parameters ) ;
			for( Neuron n : neurons ) {
				int ix  = n.getIndexInBrain() ;
				int loc[] = getLocationFromIndex(ix) ;
				if( loc[0] == 0 ) {
					double weight = 
					( rng.nextDouble() > parameters.inhibitorRatio) ?
					rng.nextDouble() : rng.nextDouble() ;

					Axon axon = new Axon( inputs[i], weight ) ;
					n.addInput(axon);
				}
			}
		}

		
		for( int i=0 ; i<outputs.length ; i++ ) {
			outputs[i] = new OutputNeuron( this, i, parameters ) ;
			for( Neuron n : neurons ) {
				int ix  = n.getIndexInBrain() ;
				int loc[] = getLocationFromIndex(ix) ;
				if( loc[0] == brainDimensions[0]-1 ) {
					double weight = 
					( rng.nextDouble() > parameters.inhibitorRatio ) ?
					rng.nextDouble() : -rng.nextDouble() ;

					Axon axon = new Axon( n, weight ) ;
					outputs[i].addInput(axon);
				}
			}
		}
		while( removeDeadReferences( neurons ) ) {} ;

		this.neurons = new Neuron[ neurons.size() ] ;
		neurons.toArray( this.neurons ) ;
		log.debug( "Copied {} neurons", this.neurons.length ) ;
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


	public void updateScores() {
		double score = 0 ;
		for( OutputNeuron n : outputs ) {
			double p = n.getPotential() ; 
			for( Neuron o : outputs ) {
				score += ( o.getPotential() - p ) * ( o.getPotential() - p ) ; 
			}
			int sl = n.spike.length ;
			double dp = p - n.getHistory( sl ) ; 
			score += dp * dp ;
		}

		if( scoreClock < scoreReservoir.length ) {
			scoreReservoir[scoreClock] = score ;
			scoreClock++ ;
		} else {
			int ix = rng.nextInt( scoreReservoir.length ) ;
			scoreReservoir[ix] = score ;
		}
	}

	/**
	 * Get the average score 
	 */
	public double getScore() {
		double score = 0 ;
		for( int i=0 ; i<scoreClock ; i++ ) {
			score += scoreReservoir[i] ;
		}
		return score / scoreClock ;
	}
	

	protected boolean removeDeadReferences( List<Neuron> neurons ) {
		boolean removed = false ;
		Set<Neuron> visited = new HashSet<>() ; 
		Queue<Neuron> queue = new LinkedList<>() ;
		
		for( Neuron output : outputs ) {
			queue.add( output ) ;
			for( Iterator<Axon> j=output.iterator() ; j.hasNext() ; ) {
				Axon a = j.next() ;
				Neuron n = a.getNeuron() ;
				if( !neurons.contains(n)  ) {
					j.remove();
					removed = true ;
				}
			}
		}
		
		while( !queue.isEmpty() ) {
			Neuron n = queue.remove() ; 
			if( visited.add(n) ) {
				for( Axon a : n ) {
					Neuron n2 = a.getNeuron() ;
					if( !n2.isDead() && neurons.contains(n2) ) {
						queue.add( n2 ) ;
					}
				}
			}
		}
		log.debug( "Visited {} neurons", visited.size() ) ;
		for( Iterator<Neuron> i=neurons.iterator() ; i.hasNext() ; ) {
			Neuron dead = i.next() ;
			if( !visited.contains( dead ) ) {
				i.remove() ;
				removed = true ;
				for( Neuron n : neurons ) {
					for( Iterator<Axon> j=n.iterator() ; j.hasNext() ; ) {
						Axon a = j.next() ;
						if( a.getNeuron() == dead ) {
							j.remove(); 
						}
					}
				}
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
		int ix = 0 ;
		rc.states = new NeuronState[ inputs.length + outputs.length + neurons.length] ;
		rc.outputs = new double[ outputs.length ] ;
		
		for( int i=0 ; i<inputs.length ; i++ ) {
			rc.states[ix++] = new NeuronState( inputs[i] ) ;
		}
		for( int i=0 ; i<outputs.length ; i++ ) {
			rc.states[ix++] = new NeuronState( outputs[i] ) ;
			rc.outputs[i] = outputs[i].getPotential() ;
		}
		for( int i=0 ; i<neurons.length ; i++ ) {
			rc.states[ix++] = new NeuronState( neurons[i] ) ;
		}
		rc.score = getScore() ;
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
			rc.append( n.getPotential() ) ;				
			rc.append( ",\"type\":\"" ) ;
			rc.append( n.getType().toString() ) ;				
			rc.append( "\",\"fx\":100" ) ;
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
			rc.append( "\",\"fx\":600" ) ;
			rc.append( " }" ) ;
		}
		for( Neuron n : neurons ) {
			int loc[] = getLocationFromIndex( n.getIndexInBrain() ) ; 
			rc.append( sep ) ;
			sep = ',' ;
			rc.append( "{\"name\":\"" ) ;
			rc.append( n.getName() ) ;
			rc.append( "\",\"potential\":" ) ;
			rc.append( Double.isFinite( n.getPotential() ) ? n.getPotential() : 0 ) ;				
			rc.append( ",\"type\":\"" ) ;
			rc.append( n.getType().toString() ) ;				
			rc.append( "\",\"px\":" ) ;
			rc.append( loc[0] ) ;
			rc.append( ",\"py\":" ) ;
			rc.append( loc[1] ) ;
			rc.append( " }" ) ;
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


	public boolean save( String fileName ) {
		boolean rc = false ;
		Gson gson = new Gson() ;
		try ( OutputStream os = new FileOutputStream( fileName ))  {
			String json = gson.toJson( parameters ) ;
			os.write( json.getBytes( "UTF-8" ) ) ;
		} catch( IOException ioe ) {
			rc = false ;
		}
		return rc ;
	}


	public static Brain load( String fileName ) {
		Brain rc  ;
		File file = new File( fileName ) ;
		try ( InputStream is = new FileInputStream(fileName) ;
			  Reader json = new InputStreamReader(is) ) {
			Gson gson = new Gson() ;
			BrainParameters bp = (BrainParameters)gson.fromJson( json, BrainParameters.class ) ;
			rc = new Brain( bp ) ;
		} catch( Exception e ) {
			e.printStackTrace();
			rc = null ;
		}
		return rc ;
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
	public double score ;
	public NeuronState states[] ;
	public double outputs[] ;
}

class NeuronState {
	public double potential ;
	public String name ;
	public NeuronState( Neuron n ) {
		this.potential = n.getPotential() ;
		this.name = n.getName() ;
	}
}

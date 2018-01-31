package com.rc ;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Brain  {

	final static Logger log = LoggerFactory.getLogger( Brain.class ) ;
	
	final static public int HISTORY_LENGTH = 100 ;

	private final double outputHistory[][] ;
	private int historyIndex ;
	
	private final Neuron neurons[] ;
	private final Edge edges[][] ;								// adjacency list ( directed & weighted )
	
	private final int [] brainDimensions ;						// the grid size(s) of the brain ( e.g. a 3x3 brain has 3 layers of 3 neurons )
	private final Neuron  [] inputs ;								// which nodes are .. inputs 
	private final Neuron  [] outputs ;							// .. and outputs

	private static final Random rng = new Random(24) ;		// utility random number generator

	private final double spikeCost ; 
	private final BrainParameters parameters ;

	private final double scoreReservoir[] ;
	private int scoreClock ;

	public Brain( BrainParameters parameters, int ... dims ) {
		this.parameters = parameters ;
		this.scoreReservoir = new double[1000] ;
		this.scoreClock = 0 ;
		this.brainDimensions = dims ;
		this.outputHistory = new double[HISTORY_LENGTH][ parameters.numOutputs] ; 
		this.historyIndex = 0 ;
	
		// OK let's create space for all neurons
		// The X0 x X1 x .... Xn 
		// plus inputs & outputs
		int numNeurons = 1 ;
		for( int i=0 ; i<brainDimensions.length ; i++ ) {
			numNeurons *= brainDimensions[i] ; 
		}
	
		numNeurons += parameters.numInputs ;
		numNeurons += parameters.numOutputs ;
		
		this.neurons = new Neuron[ numNeurons ] ;
		for( int i = 0 ; i<numNeurons ; i++ ) {
			neurons[i] = new Neuron( String.valueOf(i), parameters ) ;
		}
		
		// Remember the inputs & outputs for convenience later 
		this.inputs = new Neuron[parameters.numInputs] ;
		this.outputs = new Neuron[parameters.numOutputs] ;

		// Copy the first few neurons to input cache
		for( int i=0 ; i<inputs.length ; i++ ) {
			inputs[i] = neurons[i] ;
		}
		// Copy the last few neurons to output cache
		for( int i=0 ; i<outputs.length ; i++ ) {
			outputs[i] = neurons[numNeurons-i-1] ;
		}

		// All neurons can have edges 
		this.edges = new Edge[ numNeurons ][] ;

		// Left hand side of the core are connected to inputs
		// So get the index of all nodes where the first
		// index == 0
		int leftLiquid[] = new int[brainDimensions.length] ;
		for( int i=2 ; i<leftLiquid.length ; i++ ) {
			leftLiquid[i] = brainDimensions[i] - 1 ;
		}
		leftLiquid[0] = 0 ;
		// Now we can get all nodes that math any left liquid index
		// it's connected to an input
		for( int i=0 ; i<inputs.length ; i++ ) {
			edges[i] = new Edge[ brainDimensions[1] ] ;
			
			for( int l=0 ; l<edges[i].length ; l++ ) {
				leftLiquid[1] = l ;
				int source = getIndexFromLocation( leftLiquid ) ;
				log.debug( "Neuron[{}] @ {} is connected to input {}", source, leftLiquid, i) ;
				double weight = rng.nextDouble() *
					rng.nextDouble() < parameters.inhibitorRatio ? -1 :1 ;
				edges[i][l] = new Edge( source, weight ) ;
			}
		}
		
		// Repeat the same thing - but for outputs ...
		leftLiquid[0] = brainDimensions[0] - 1 ;
		
		for( int i=0 ; i<outputs.length ; i++ ) {
			edges[numNeurons-i-1] = new Edge[ brainDimensions[1] ] ;
			
			for( int l=0 ; l<edges[numNeurons-i-1].length ; l++ ) {
				leftLiquid[1] = l ;
				int source = getIndexFromLocation( leftLiquid ) ;
				log.debug( "Neuron[{}] @ {} is connected to output {}", source, leftLiquid, i) ;
				double weight = rng.nextDouble() *
					rng.nextDouble() < parameters.inhibitorRatio ? -1 :1 ;
				edges[numNeurons-i-1][l] = new Edge( source, weight ) ;
			}
		}

		List<Edge> newEdges = new ArrayList<Edge>() ;

		for( int targetNeuronIndex = inputs.length ; targetNeuronIndex<(numNeurons-outputs.length) ; targetNeuronIndex++ ) {
			final int loc[] = getLocationFromIndex( targetNeuronIndex ) ;
			newEdges.clear();
			
			for( int d=0 ; d<dims.length ; d++ ) {
				loc[d]-- ;
				if( loc[d] >= 0 ) {
					double weight = rng.nextDouble() *
						rng.nextDouble() < parameters.inhibitorRatio ? -1 :1 ;
					Edge edge = new Edge( getIndexFromLocation(loc), weight ) ;
					newEdges.add( edge ) ;
				}
				loc[d]+=2 ;
				if( loc[d] < dims[d] ) {
					double weight = rng.nextDouble() *
						rng.nextDouble() < parameters.inhibitorRatio ? -1 :1 ;
					Edge edge = new Edge( getIndexFromLocation(loc), weight ) ;
					newEdges.add( edge ) ;
				}
				loc[d]-- ;
			}	
			for( int i=0 ; i<inputs.length ; i++ ) {				
				Edge inputEdges[] = edges[i] ;
				for( int j=0 ; j<inputEdges.length ; j++ ) {
					if( inputEdges[j].source == targetNeuronIndex ) {
						newEdges.add( new Edge( i, inputEdges[j].weight ) ) ;
					}
				}
			}
			edges[targetNeuronIndex] = new Edge[ newEdges.size() ] ;
			newEdges.toArray( edges[targetNeuronIndex] ) ;
		}		
		// Clear out inputs to actual inputs - it's wrong !
		for( int i=0 ; i<inputs.length ; i++ ) {				
			edges[i] = new Edge[0] ;
		}		
		// Better score if spike is flat
		double sc = 0 ;
		for( double s : parameters.spikeProfile ) {
			sc += s ;
		}
		this.spikeCost = sc ; //sc / ( 3.0 * inputs.length ) ;
	}

	public void step( double[] inputs ) {

		// Set inputs immediately - no dependencies
		for( int i=0 ; i<inputs.length ; i++ ) {
			this.inputs[i].setPotential( inputs[i] );
		}
		
		// Will build up all outputs - before chaning any of them
		double newPotentials[] = new double[ neurons.length ] ;
		
		for( int n=inputs.length ; n<neurons.length; n++ ) {
			newPotentials[n] = 0 ;
			for( int e=0 ; e<edges[n].length ; e++ ) {
				Edge edge = edges[n][e] ;
				newPotentials[n] += neurons[edge.source].getPotential() * edge.weight ;
			}
		}
		
		// Then write the output as an atomic op
		for( int n=inputs.length ; n<neurons.length; n++ ) {
			neurons[n].setPotential( newPotentials[n] ) ;
		}
		
		// For scoring
		for( int i=0 ; i<outputs.length ; i++ ) {
			outputHistory[historyIndex][i] = outputs[i].getPotential() ;
		}
	}


	// High score is better for survival
	public void updateScores() {
		double score = 0 ; //-Math.abs(spikeCost) ;
		for( int n=0 ; n<outputs.length ; n++  ) {
			double p = neurons[n].getPotential() ; 
			score += p ;
			double tmp = 0 ;
			for( Neuron o : outputs ) {
				double dp = o.getPotential() - p ;
				tmp += dp * dp  ; 
			}
			score += Math.sqrt( tmp ) ;
			double dp = p - getHistory( n, BrainParameters.SPIKE_PROFILE_SIZE ) ; 
			//score += Math.abs(dp) ;
		}

		if( scoreClock < scoreReservoir.length ) {
			scoreReservoir[scoreClock] = score ;
			scoreClock++ ;
		} else {
			int ix = rng.nextInt( scoreReservoir.length ) ;
			scoreReservoir[ix] = score ;
		}
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
		double maxScore = scoreReservoir[0] ;
		double minScore = scoreReservoir[0] ;
		double avgScore = scoreReservoir[0] ;
		for( int i=1 ; i<scoreClock ; i++ ) {
			maxScore = Math.max( maxScore, scoreReservoir[i] ) ;
			minScore = Math.min( maxScore, scoreReservoir[i] ) ;
			avgScore += scoreReservoir[i] ;
		}
		avgScore /= scoreClock ;
		return avgScore  ;
	}


	/**
	 * Determine the coordinates of a neuron in the brain
	 */
	public int[] getLocationFromIndex( int index ) {
		index -= inputs.length  ;
		
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

		return rc += inputs.length ;
	}


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

	public Neuron[] getInputs() { return inputs ; }
	public Neuron[] getOutputs() { return outputs ; }

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
		for( int n=0 ; n<neurons.length ; n++ ) {
			rc.append( sep ) 
			.append( "{ \"name\":\"" ) 
			.append( neurons[n].getName() ) 
			.append( "\",\"potential\":" ) 
			.append( neurons[n].getPotential() ) 
			;
			if( n < inputs.length ) {
				rc.append( ",\"fx\": 0 ") ;
			} else if ( n>=(neurons.length - outputs.length) ) {
				rc.append( ",\"fx\": 800 ") ;
			}
			rc.append( " }" ) 
			;
			sep = ',' ;
		}
	}

	private void printLinks( StringBuilder rc ) {
		char sep = ' '  ;
		for( int n=0 ; n<neurons.length ; n++  ) {
			for( int e=0 ; e<edges[n].length ; e++ ) {
				Edge edge = edges[n][e] ;
				Neuron source = neurons[edge.source] ;
				Neuron target = neurons[n] ;
				
				rc.append( sep ) 
				.append( "{\"source\":\"" ) 		
				.append( source.getName() ) 
				.append( "\",\"target\":\"" ) 
				.append( target.getName() ) 
				.append( "\",\"weight\":" ) 
				.append( edge.weight ) 		
				.append( " }" ) 
				;
				sep = ',' ;
			}
		}
	}


	public boolean save( String fileName ) {
		boolean rc = false ;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try ( OutputStream os = new FileOutputStream( fileName ))  {
			parameters.dimensions = this.brainDimensions ;
			String json = gson.toJson( parameters ) ;
			os.write( json.getBytes( "UTF-8" ) ) ;
		} catch( IOException ioe ) {
			rc = false ;
		}
		return rc ;
	}


	public static Brain load( String fileName ) {
		return Brain.load( fileName, 0, 0 ) ;
	}		

	public static Brain load( String fileName, int ... dims ) {
		Brain rc  ;
		try ( InputStream is = new FileInputStream(fileName) ;
				Reader json = new InputStreamReader(is) ) {
			Gson gson = new Gson() ;
			BrainParameters bp = (BrainParameters)gson.fromJson( json, BrainParameters.class ) ;
			rc = new Brain( bp, dims ) ;
		} catch( Exception e ) {
			e.printStackTrace();
			rc = null ;
		}
		return rc ;
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

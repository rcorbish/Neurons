package com.rc;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rc.neurons.Neuron;


public class TestBrain {

	final double TICK = 1e-5 ;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}


	@Test
	public void testBrainSaveSummary() {
		// create a brain with 
		//	2 inputs
		//	5 outputs
		//  4 x 6 liquid
		Brain b = new Brain( TICK, 2, 4, 4, 6 ) ;
		
		Genome g = b.toGenome() ;
		Brain br = new Brain( TICK, g ) ;

		double accuracy = g.accuracy() ;

		assertEquals( "Invalid num rows recovered", b.getRows(), br.getRows() )  ;
		assertEquals( "Invalid num cols recovered", b.getColumns(), br.getColumns() )  ;
		assertEquals( "Invalid num neurons recovered", b.numNeurons(), br.numNeurons() )  ;
		
		for( int i=0 ; i<b.numNeurons() ; i++ ) {
			Neuron nr = br.getNeuron(i) ;
			Neuron n  =  b.getNeuron(i) ;

			assertEquals( "Invalid neuron recovered - type", n.getType(), nr.getType())  ;
			assertEquals( "Invalid neuron recovered - ID", n.getId(), nr.getId() )  ;
		}
	}

	/*


	@Test
	public void testBrainGenealogy() {
		// create a brain with 
		//	2 inputs
		//	5 outputs
		//  4 x 6 liquid

		Brain b = new Brain( TICK, 2, 4, 6 ) ;
		Genome g = b.toGenome() ;
		Genome c = new Genome( g, g, 0 ) ;

		System.out.println( "G:" + g + "\nC:" + c ) ;
		Brain br = new Brain( TICK, c ) ;
		double accuracy = g.accuracy() ;
		
		assertEquals( "Invalid num neuron layers recovered", b.getNumLayers(), br.getNumLayers() )  ;  
		assertEquals( "Invalid num neurons recovered", b.numNeurons(), br.numNeurons() )  ;  
		
		for( int i=0 ; i<b.numNeurons() ; i++ ) {
			Neuron nr = br.getNeuron(i) ;
			Neuron n  =  b.getNeuron(i) ;

			assertEquals( "Invalid neuron recovered - threshold", n.getThreshold(), nr.getThreshold(), accuracy )  ;
			assertEquals( "Invalid neuron recovered - resting", n.getRestingPotential(), nr.getRestingPotential(), accuracy )  ;
			assertEquals( "Invalid neuron recovered - learning rate", n.getLearningRate(), nr.getLearningRate(), accuracy )  ;
			assertEquals( "Invalid neuron recovered - ID", n.getId(), nr.getId() )  ;
		}
		
		for( int i=0 ; i<b.numNeurons() ; i++ ) {
			EdgeList elr  = br.getIncomingEdges(i) ;
			EdgeList el  = b.getIncomingEdges(i) ;

			assertEquals( "Invalid incoming edgeList recovered " + i , el.size(), elr.size() ) ;
			
			for( int j=0 ; j<el.size() ; j++ ) {
				assertEquals( "Invalid edge recovered " + i + "," + j, el.get(j).weight(), elr.get(j).weight(), accuracy ) ;
				assertEquals( "Invalid edge recovered " + i + "," + j, el.get(j).source(), elr.get(j).source() ) ;
			}
			
			elr  = br.getOutgoingEdges(i) ;
			el   = b.getOutgoingEdges(i) ;

			assertEquals( "Invalid outgoing edgeList recovered " + i , el.size(), elr.size() ) ;
			
			for( int j=0 ; j<el.size() ; j++ ) {
				assertEquals( "Invalid edge recovered " + i + "," + j, el.get(j).weight(), elr.get(j).weight(), accuracy ) ;
				assertEquals( "Invalid edge recovered " + i + "," + j, el.get(j).source(), elr.get(j).source() ) ;
			}
		}
	}
*/
}

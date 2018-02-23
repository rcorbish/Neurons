package com.rc;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		Brain b = new Brain( TICK, 2, 4, 6 ) ;
		
		Genome g = b.toGenome() ;
		Brain br = new Brain( TICK, g ) ;
		double accuracy = g.accuracy() ;
		
		assertEquals( "Invalid num neuron layers recovered", b.getNumLayers(), br.getNumLayers() )  ;  
		
		int numNeurons = 0 ;
		for( int i=0 ; i<b.getNumLayers() ; i++ ) {
			assertEquals( "Invalid num neurons in layer " + i + " recovered", b.getLayer(i).length, br.getLayer(i).length )  ;  
			for( int j=0 ; j<b.getLayer(i).length ; j++, numNeurons++ ) {
				Neuron nr = br.getNeuron(i,j) ;
				Neuron n  =  b.getNeuron(i,j) ;
	
				assertEquals( "Invalid neuron recovered", n.getDecay(), nr.getDecay(), accuracy )  ;
				assertEquals( "Invalid neuron recovered", n.getThreshold(), nr.getThreshold(), accuracy )  ;
				assertEquals( "Invalid neuron recovered", n.getRestingPotential(), nr.getRestingPotential(), accuracy )  ;
				assertEquals( "Invalid neuron recovered", n.getId(), nr.getId() )  ;
			}
		}
		
		for( int i=0 ; i<numNeurons ; i++ ) {
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



	@Test
	public void testBrainGenealogy() {
		// create a brain with 
		//	2 inputs
		//	5 outputs
		//  4 x 6 liquid

		Brain b = new Brain( TICK, 2, 4, 6 ) ;
		Genome g = b.toGenome() ;
		Genome c = new Genome( g, g, 0 ) ;

		Brain br = new Brain( TICK, c ) ;
		double accuracy = g.accuracy() ;
		
		assertEquals( "Invalid num neuron layers recovered", b.getNumLayers(), br.getNumLayers() )  ;  
		
		int numNeurons = 0 ;
		for( int i=0 ; i<b.getNumLayers() ; i++ ) {
			assertEquals( "Invalid num neurons in layer " + i + " recovered", b.getLayer(i).length, br.getLayer(i).length )  ;  
			for( int j=0 ; j<b.getLayer(i).length ; j++, numNeurons++ ) {
				Neuron nr = br.getNeuron(i,j) ;
				Neuron n  =  b.getNeuron(i,j) ;
	
				assertEquals( "Invalid neuron recovered", n.getDecay(), nr.getDecay(), accuracy )  ;
				assertEquals( "Invalid neuron recovered", n.getThreshold(), nr.getThreshold(), accuracy )  ;
				assertEquals( "Invalid neuron recovered", n.getRestingPotential(), nr.getRestingPotential(), accuracy )  ;
				assertEquals( "Invalid neuron recovered", n.getId(), nr.getId() )  ;
			}
		}
		
		for( int i=0 ; i<numNeurons ; i++ ) {
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

}

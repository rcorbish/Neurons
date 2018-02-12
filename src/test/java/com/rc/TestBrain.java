package com.rc;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestBrain {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}


	@Test
	public void testBrainSaveDetail() {
		// create a brain with 
		//	5 inputs
		//	4 outputs
		//  3 x 3 liquid
		Brain b = new Brain( 5, 3, 4 ) ;
		
		Genome g = b.toGenome() ;
		
		double accuracy = g.accuracy() ;
		
		int nl = g.getInt( 0 ) ;		
		assertEquals( "Invalid num layers recovered", 3, nl )  ;  // 3, 4, 5
		
		int sz = b.getNumNeurons() ;
		
		for( int i=0 ; i<sz ; i++ ) {
			Genome gr = g.subSequence( b.GENOME_SIZE+(i*Neuron.GENOME_SIZE), Neuron.GENOME_SIZE ) ;
			Neuron nr = new Neuron( gr ) ;
			Neuron n = b.getNeuron(i) ;

			assertEquals( "Invalid neuron recovered", n.decay, nr.decay, accuracy )  ;
			assertEquals( "Invalid neuron recovered", n.threshold, nr.threshold, accuracy )  ;
			assertEquals( "Invalid neuron recovered", n.restingPotential, nr.restingPotential, accuracy )  ;
			assertEquals( "Invalid neuron recovered", n.index, nr.index )  ;
		}

		int start = b.GENOME_SIZE+(sz*Neuron.GENOME_SIZE) ;
		for( int i=0 ; i<sz ; i++ ) {
			int numEdges = g.getInt( start ) ;
			
			Genome gr = g.subSequence( start, 1 + numEdges * Edge.GENOME_SIZE ) ;
			EdgeList elr = new EdgeList( gr ) ;
			EdgeList el  = b.getIncomingEdges(i) ;

			assertEquals( "Invalid edgeList recovered " + i , el.size(), elr.size() ) ;
			
			for( int j=0 ; j<el.size() ; j++ ) {
				assertEquals( "Invalid edge recovered " + i + "," + j, el.get(j).weight(), elr.get(j).weight(), accuracy ) ;
				assertEquals( "Invalid edge recovered " + i + "," + j, el.get(j).source(), elr.get(j).source() ) ;
			}

			start += numEdges * Edge.GENOME_SIZE + 1 ;
		}
	}

	
	@Test
	public void testBrainSaveSummary() {
		// create a brain with 
		//	2 inputs
		//	5 outputs
		//  4 x 6 liquid
		Brain b = new Brain( 2, 4, 6 ) ;
		
		Genome g = b.toGenome() ;
		Brain br = new Brain( g ) ;
		double accuracy = g.accuracy() ;
		
		assertEquals( "Invalid num neurons recovered", b.getNumNeurons(), br.getNumNeurons() )  ;  
		
		int start = b.GENOME_SIZE  ;
		for( int i=0 ; i<b.getNumNeurons() ; i++ ) {
			Genome gr = g.subSequence( start, Neuron.GENOME_SIZE ) ;
			Neuron nr = new Neuron( gr ) ;
			Neuron n = b.getNeuron(i) ;

			assertEquals( "Invalid neuron recovered", n.decay, nr.decay, accuracy )  ;
			assertEquals( "Invalid neuron recovered", n.threshold, nr.threshold, accuracy )  ;
			assertEquals( "Invalid neuron recovered", n.restingPotential, nr.restingPotential, accuracy )  ;
			assertEquals( "Invalid neuron recovered", n.index, nr.index )  ;
			
			start += Neuron.GENOME_SIZE  ;
		}
		
		for( int i=0 ; i<b.getNumNeurons() ; i++ ) {
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

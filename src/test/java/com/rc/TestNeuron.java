package com.rc;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestNeuron {

	BrainParameters bp ;

	@Before
	public void setUp() throws Exception {
		bp = new BrainParameters() ;
	}

	@After
	public void tearDown() throws Exception {
		bp = null ;
	}


	@Test
	public void testGenomeInt() {
		Genome g = new Genome( 2 ) ;

		int v = 31 ;
		g.set( v, 0 ) ;
		int w = g.getInt( 0 ) ;
		assertEquals( "Bitset int - invalid integer", v, w) ;

		int v2 = 37 ;
		g.set( v2, 1 ) ;
		int w2 = g.getInt( 1 ) ;
		assertEquals( "Bitset int - invalid integer", v2, w2) ;

		int w3 = g.getInt( 0 ) ;
		assertEquals( "Bitset int - invalid integer", v, w3) ;

		int v4 = 27 ;
		g.set( v4, 2 ) ;
		int w4 = g.getInt( 2 ) ;
		assertEquals( "Bitset int - invalid integer", v4, w4) ;
	}
	
	@Test
	public void testGenomeDouble() {
		Genome g = new Genome( 2 ) ;
		double accuracy = g.accuracy() ;

		double v = .31 ;
		g.set( v, 0 ) ;
		double w = g.getDouble( 0 ) ;
		assertEquals( "Bitset double - invalid value 1", v, w, accuracy ) ;

		double v2 = .37 ;
		g.set( v2, 1 ) ;
		double w2 = g.getDouble( 1 ) ;
		assertEquals( "Bitset double - invalid value 2", v2, w2, accuracy ) ;

		double w3 = g.getDouble( 0 ) ;
		assertEquals( "Bitset double - invalid value 3", v, w3, accuracy ) ;

		double v4 = .27 ;
		g.set( v4, 2 ) ;
		double w4 = g.getDouble( 2 ) ;
		assertEquals( "Bitset double - invalid value 4", v4, w4, accuracy ) ;
	}
	
	@Test
	public void testGenomeSubsequence() {
		Genome g = new Genome( 30 ) ;
		double accuracy = g.accuracy() ;
		for( int i=0 ;i<30 ; i++ ) {
			g.set( i, i ) ;
		}
		for( int i=0 ; i<20 ; i++ ) {
			Genome g2 = g.subSequence(i, 3) ;
			int v = g2.getInt( 1 ) ;
			assertEquals( "Bitset - invalid subsequence", i+1, v ) ;
			int v2 = g2.getInt( 2 ) ;
			assertEquals( "Bitset - invalid subsequence", i+2, v2 ) ;
		}
	}

	@Test
	public void testNeuron() {
		Neuron n = new Neuron("TEST", bp ) ;
		Genome g = n.toGenome() ;
		Neuron n2 = new Neuron( n.getName(), g ) ;
		double accuracy = g.accuracy() ;
		assertEquals( "Bitset copy - bad decay", n.decay, n2.decay, accuracy ) ;
		assertEquals( "Bitset copy - bad threshold", n.threshold, n2.threshold, accuracy ) ;
		assertEquals( "Bitset copy - bad resting potential", n.restingPotential, n2.restingPotential, accuracy ) ;
	}
	
	
}

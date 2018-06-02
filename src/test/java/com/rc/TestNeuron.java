package com.rc;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.rc.neurons.Neuron;
import com.rc.neurons.NeuronRS;

@Ignore
public class TestNeuron {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}


	@Test
	public void testGenomeInt() {
		Genome g = new Genome() ;

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
		Genome g = new Genome() ;
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
	}

	@Test
	public void testNeuron() {
		Neuron n = new NeuronRS( 1 ) ;
	}
	
	
}

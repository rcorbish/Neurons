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

package com.rc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EdgeList implements Iterable<Edge> {

	private final List<Edge> edges ;

	
	public EdgeList() {
		edges = new ArrayList<>() ;
	}

	public EdgeList( Genome genome ) {
		this() ;
		int n = genome.getInt(0) ;
		for( int i=0 ; i<n ; i++ ) {
			Genome g = genome.subSequence( i*Edge.GENOME_SIZE+1, Edge.GENOME_SIZE ) ;
			edges.add(  new Edge( g ) ) ;			
		}
	}
	
	public EdgeList( List<Edge> edges ) {
		this() ;
		this.edges.addAll( edges ) ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( edges.size(), 0 ) ;
		for( int i=0 ; i<edges.size() ; i++ ) {
			rc.append( edges.get(i).toGenome() ) ;
		}
		return rc ;
	}
	
	public void set( int index, Edge e ) {
		edges.set( index, e ) ;
	}
	
	public void add( Edge e ) {
		edges.add( e ) ;
	}
	
	public Edge get( int index  ) {
		return edges.get( index ) ;
	}
	
	public void clear() {
		edges.clear();
	}
	
	public int size() {
		return edges.size() ;
	}

	@Override
	public Iterator<Edge> iterator() {
		return edges.iterator() ;
	}
}

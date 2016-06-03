package net.akehurst.language.graphStructuredStack.impl;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.graphStructuredStack.IGssNode;

public class GssNode<K, V> implements IGssNode<K, V> {
	public GssNode(GraphStructuredStack<K, V> gss, K key, V value) {
		this.gss = gss;
		this.key = key;
		this.value = value;
		this.next = new ArrayList<>();
		this.previous = new ArrayList<>();
	}

	GraphStructuredStack<K, V> gss;

	K key;

	@Override
	public K getKey() {
		return this.key;
	}

	V value;

	@Override
	public V getValue() {
		return this.value;
	}

	@Override
	public void push(K key, V value) {
		GssNode<K, V> next = this.gss.newNode(key, value);
		next.previous().add(this);
		this.next().add(next);
		//TODO: performance!
		if (gss.getTops().contains(this)) {
			gss.getTops().remove(this);
			gss.getTops().add(next);
		}
	}

	List<IGssNode<K, V>> next;

	@Override
	public List<IGssNode<K, V>> next() {
		return this.next;
	}

	List<IGssNode<K, V>> previous;

	@Override
	public List<IGssNode<K, V>> previous() {
		return this.previous;
	}

	@Override
	public boolean hasPrevious() {
		return !this.previous().isEmpty();
	}

	@Override
	public boolean hasNext() {
		return !this.next().isEmpty();
	}
	
//	@Override
//	public int hashCode() {
//		return this.getKey().hashCode();
//	}
//	
//	@Override
//	public boolean equals(Object obj) {
//		if ()
//		return super.equals(obj);
//	}
	
	@Override
	public String toString() {
		return this.getKey().toString();
	}
}

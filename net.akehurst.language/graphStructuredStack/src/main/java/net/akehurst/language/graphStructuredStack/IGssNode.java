package net.akehurst.language.graphStructuredStack;

import java.util.List;

public interface IGssNode<K,V> {
	K getKey();
	V getValue();
	
	/**
	 * push a new node into the GSS,
	 * if it already exists in the graph, add it to the same node
	 * else push a new node on the top of the stack with this node as the previous.
	 * 
	 * @param key
	 * @param value
	 */
	void push(K key, V value);
	
	List<IGssNode<K, V>> next();
	List<IGssNode<K, V>> previous();
	
	boolean hasPrevious();
	boolean hasNext();
}

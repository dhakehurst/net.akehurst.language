package net.akehurst.language.graphStructuredStack;

import java.util.List;

public interface IGraphStructuredStack<K,V> {

	/**
	 * The tops of the stacks
	 * @return
	 */
	List<IGssNode<K,V>> getTops();
	
	IGssNode<K,V> newBottom(K key, V value);

	IGssNode<K,V> peek(K key);

	void pop(K key);

	void addTop(K key, V value);
}
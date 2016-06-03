package net.akehurst.language.graphStructuredStack;

import java.util.List;

public interface IGraphStructuredStack<K,V> {

	/**
	 * The tops of the stacks
	 * @return
	 */
	List<IGssNode<K,V>> getTops();
	
	IGssNode<K,V> newBottom(K key, V value);

	IGraphStructuredStack<K,V> shallowClone();

	IGssNode<K,V> peek(K key);
}

/**
 * 06.06.2008
 */
package de.freese.knn.net.layer;

import java.util.List;

import de.freese.base.core.visitor.IVisitable;
import de.freese.knn.net.function.IFunction;
import de.freese.knn.net.matrix.Matrix;
import de.freese.knn.net.neuron.INeuron;

/**
 * Interface eines Layers.
 * 
 * @author Thomas Freese
 */
public interface ILayer extends IVisitable
{
	/**
	 * Liefert die Funktion des Layers.
	 * 
	 * @return {@link IFunction}
	 */
	public IFunction getFunction();

	/**
	 * Liefert die Eingangsmatrix des Layers.
	 * 
	 * @return {@link Matrix}
	 */
	public Matrix getInputMatrix();

	/**
	 * Liefert die Neuronen des Layers.
	 * 
	 * @return {@link List}
	 */
	public List<INeuron> getNeurons();

	/**
	 * Liefert die Ausgangsmatrix des Layers.
	 * 
	 * @return {@link Matrix}
	 */
	public Matrix getOutputMatrix();

	/**
	 * Liefert die Anzahl der Neuronen.
	 * 
	 * @return int
	 */
	public int getSize();

	/**
	 * Setzt die Eingangsmatrix des Layers.
	 * 
	 * @param matrix {@link Matrix}
	 */
	public void setInputMatrix(Matrix matrix);

	/**
	 * Setzt die Ausgangsmatrix des Layers.
	 * 
	 * @param matrix {@link Matrix}
	 */
	public void setOutputMatrix(Matrix matrix);
}

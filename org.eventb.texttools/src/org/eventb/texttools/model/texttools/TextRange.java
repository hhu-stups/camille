/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.eventb.texttools.model.texttools;

import java.util.Map;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Text Range</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.eventb.texttools.model.texttools.TextRange#getOffset <em>Offset</em>}</li>
 *   <li>{@link org.eventb.texttools.model.texttools.TextRange#getLength <em>Length</em>}</li>
 *   <li>{@link org.eventb.texttools.model.texttools.TextRange#getSubTextRanges <em>Sub Text Ranges</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.eventb.texttools.model.texttools.TexttoolsPackage#getTextRange()
 * @model
 * @generated
 */
public interface TextRange extends EObject {
	/**
	 * Returns the value of the '<em><b>Offset</b></em>' attribute.
	 * The default value is <code>"0"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Offset</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Offset</em>' attribute.
	 * @see #setOffset(int)
	 * @see org.eventb.texttools.model.texttools.TexttoolsPackage#getTextRange_Offset()
	 * @model default="0" required="true" transient="true"
	 * @generated
	 */
	int getOffset();

	/**
	 * Sets the value of the '{@link org.eventb.texttools.model.texttools.TextRange#getOffset <em>Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Offset</em>' attribute.
	 * @see #getOffset()
	 * @generated
	 */
	void setOffset(int value);

	/**
	 * Returns the value of the '<em><b>Length</b></em>' attribute.
	 * The default value is <code>"0"</code>.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Length</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Length</em>' attribute.
	 * @see #setLength(int)
	 * @see org.eventb.texttools.model.texttools.TexttoolsPackage#getTextRange_Length()
	 * @model default="0" required="true" transient="true"
	 * @generated
	 */
	int getLength();

	/**
	 * Sets the value of the '{@link org.eventb.texttools.model.texttools.TextRange#getLength <em>Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Length</em>' attribute.
	 * @see #getLength()
	 * @generated
	 */
	void setLength(int value);

	/**
	 * Returns the value of the '<em><b>Sub Text Ranges</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Sub Text Ranges</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Sub Text Ranges</em>' attribute.
	 * @see #setSubTextRanges(Map)
	 * @see org.eventb.texttools.model.texttools.TexttoolsPackage#getTextRange_SubTextRanges()
	 * @model required="true" transient="true"
	 * @generated
	 */
	Map<String, TextRange> getSubTextRanges();

	/**
	 * Sets the value of the '{@link org.eventb.texttools.model.texttools.TextRange#getSubTextRanges <em>Sub Text Ranges</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Sub Text Ranges</em>' attribute.
	 * @see #getSubTextRanges()
	 * @generated
	 */
	void setSubTextRanges(Map<String, TextRange> value);

} // TextRange

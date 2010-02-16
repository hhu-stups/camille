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
package org.eventb.texttools.model.texttools.impl;

import java.util.Map;
import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.EModelElementImpl;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eventb.texttools.model.texttools.TextRange;
import org.eventb.texttools.model.texttools.TexttoolsPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Text Range</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eventb.texttools.model.texttools.impl.TextRangeImpl#getOffset <em>Offset</em>}</li>
 *   <li>{@link org.eventb.texttools.model.texttools.impl.TextRangeImpl#getLength <em>Length</em>}</li>
 *   <li>{@link org.eventb.texttools.model.texttools.impl.TextRangeImpl#getSubTextRanges <em>Sub Text Ranges</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class TextRangeImpl extends EObjectImpl implements TextRange {
	/**
	 * The default value of the '{@link #getOffset() <em>Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOffset()
	 * @generated
	 * @ordered
	 */
	protected static final int OFFSET_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getOffset() <em>Offset</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOffset()
	 * @generated
	 * @ordered
	 */
	protected int offset = OFFSET_EDEFAULT;

	/**
	 * The default value of the '{@link #getLength() <em>Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLength()
	 * @generated
	 * @ordered
	 */
	protected static final int LENGTH_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getLength() <em>Length</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLength()
	 * @generated
	 * @ordered
	 */
	protected int length = LENGTH_EDEFAULT;

	/**
	 * The cached value of the '{@link #getSubTextRanges() <em>Sub Text Ranges</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSubTextRanges()
	 * @generated
	 * @ordered
	 */
	protected Map<String, TextRange> subTextRanges;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TextRangeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TexttoolsPackage.Literals.TEXT_RANGE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setOffset(int newOffset) {
		int oldOffset = offset;
		offset = newOffset;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TexttoolsPackage.TEXT_RANGE__OFFSET, oldOffset, offset));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getLength() {
		return length;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLength(int newLength) {
		int oldLength = length;
		length = newLength;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TexttoolsPackage.TEXT_RANGE__LENGTH, oldLength, length));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Map<String, TextRange> getSubTextRanges() {
		return subTextRanges;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setSubTextRanges(Map<String, TextRange> newSubTextRanges) {
		Map<String, TextRange> oldSubTextRanges = subTextRanges;
		subTextRanges = newSubTextRanges;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, TexttoolsPackage.TEXT_RANGE__SUB_TEXT_RANGES, oldSubTextRanges, subTextRanges));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case TexttoolsPackage.TEXT_RANGE__OFFSET:
				return new Integer(getOffset());
			case TexttoolsPackage.TEXT_RANGE__LENGTH:
				return new Integer(getLength());
			case TexttoolsPackage.TEXT_RANGE__SUB_TEXT_RANGES:
				return getSubTextRanges();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case TexttoolsPackage.TEXT_RANGE__OFFSET:
				setOffset(((Integer)newValue).intValue());
				return;
			case TexttoolsPackage.TEXT_RANGE__LENGTH:
				setLength(((Integer)newValue).intValue());
				return;
			case TexttoolsPackage.TEXT_RANGE__SUB_TEXT_RANGES:
				setSubTextRanges((Map<String, TextRange>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case TexttoolsPackage.TEXT_RANGE__OFFSET:
				setOffset(OFFSET_EDEFAULT);
				return;
			case TexttoolsPackage.TEXT_RANGE__LENGTH:
				setLength(LENGTH_EDEFAULT);
				return;
			case TexttoolsPackage.TEXT_RANGE__SUB_TEXT_RANGES:
				setSubTextRanges((Map<String, TextRange>)null);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case TexttoolsPackage.TEXT_RANGE__OFFSET:
				return offset != OFFSET_EDEFAULT;
			case TexttoolsPackage.TEXT_RANGE__LENGTH:
				return length != LENGTH_EDEFAULT;
			case TexttoolsPackage.TEXT_RANGE__SUB_TEXT_RANGES:
				return subTextRanges != null;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (offset: ");
		result.append(offset);
		result.append(", length: ");
		result.append(length);
		result.append(", subTextRanges: ");
		result.append(subTextRanges);
		result.append(')');
		return result.toString();
	}

} //TextRangeImpl

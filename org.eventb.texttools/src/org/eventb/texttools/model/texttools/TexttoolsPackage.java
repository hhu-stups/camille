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

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

/**
 * <!-- begin-user-doc --> The <b>Package</b> for the model. It contains
 * accessors for the meta objects to represent
 * <ul>
 * <li>each class,</li>
 * <li>each feature of each class,</li>
 * <li>each enum,</li>
 * <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * 
 * @see org.eventb.texttools.model.texttools.TexttoolsFactory
 * @model kind="package"
 * @generated
 */
public interface TexttoolsPackage extends EPackage {
	/**
	 * The package name. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	String eNAME = "texttools";

	/**
	 * The package namespace URI. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	String eNS_URI = "http://emf.eventb.org/models/core/texttools";

	/**
	 * The package namespace name. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	String eNS_PREFIX = "texttools";

	/**
	 * The singleton instance of the package. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	TexttoolsPackage eINSTANCE = org.eventb.texttools.model.texttools.impl.TexttoolsPackageImpl
			.init();

	/**
	 * The meta object id for the '
	 * {@link org.eventb.texttools.model.texttools.impl.TextRangeImpl
	 * <em>Text Range</em>}' class. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @see org.eventb.texttools.model.texttools.impl.TextRangeImpl
	 * @see org.eventb.texttools.model.texttools.impl.TexttoolsPackageImpl#getTextRange()
	 * @generated
	 */
	int TEXT_RANGE = 0;

	/**
	 * The feature id for the '<em><b>Offset</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */
	int TEXT_RANGE__OFFSET = 0;

	/**
	 * The feature id for the '<em><b>Length</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */
	int TEXT_RANGE__LENGTH = 1;

	/**
	 * The feature id for the '<em><b>Sub Text Ranges</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */
	int TEXT_RANGE__SUB_TEXT_RANGES = 2;

	/**
	 * The number of structural features of the '<em>Text Range</em>' class.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 * @ordered
	 */
	int TEXT_RANGE_FEATURE_COUNT = 3;

	/**
	 * Returns the meta object for class '
	 * {@link org.eventb.texttools.model.texttools.TextRange
	 * <em>Text Range</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the meta object for class '<em>Text Range</em>'.
	 * @see org.eventb.texttools.model.texttools.TextRange
	 * @generated
	 */
	EClass getTextRange();

	/**
	 * Returns the meta object for the attribute '
	 * {@link org.eventb.texttools.model.texttools.TextRange#getOffset
	 * <em>Offset</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the meta object for the attribute '<em>Offset</em>'.
	 * @see org.eventb.texttools.model.texttools.TextRange#getOffset()
	 * @see #getTextRange()
	 * @generated
	 */
	EAttribute getTextRange_Offset();

	/**
	 * Returns the meta object for the attribute '
	 * {@link org.eventb.texttools.model.texttools.TextRange#getLength
	 * <em>Length</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the meta object for the attribute '<em>Length</em>'.
	 * @see org.eventb.texttools.model.texttools.TextRange#getLength()
	 * @see #getTextRange()
	 * @generated
	 */
	EAttribute getTextRange_Length();

	/**
	 * Returns the meta object for the attribute '
	 * {@link org.eventb.texttools.model.texttools.TextRange#getSubTextRanges
	 * <em>Sub Text Ranges</em>}'. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the meta object for the attribute '<em>Sub Text Ranges</em>'.
	 * @see org.eventb.texttools.model.texttools.TextRange#getSubTextRanges()
	 * @see #getTextRange()
	 * @generated
	 */
	EAttribute getTextRange_SubTextRanges();

	/**
	 * Returns the factory that creates the instances of the model. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	TexttoolsFactory getTexttoolsFactory();

	/**
	 * <!-- begin-user-doc --> Defines literals for the meta objects that
	 * represent
	 * <ul>
	 * <li>each class,</li>
	 * <li>each feature of each class,</li>
	 * <li>each enum,</li>
	 * <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '
		 * {@link org.eventb.texttools.model.texttools.impl.TextRangeImpl
		 * <em>Text Range</em>}' class. <!-- begin-user-doc --> <!--
		 * end-user-doc -->
		 * 
		 * @see org.eventb.texttools.model.texttools.impl.TextRangeImpl
		 * @see org.eventb.texttools.model.texttools.impl.TexttoolsPackageImpl#getTextRange()
		 * @generated
		 */
		EClass TEXT_RANGE = eINSTANCE.getTextRange();

		/**
		 * The meta object literal for the '<em><b>Offset</b></em>' attribute
		 * feature. <!-- begin-user-doc --> <!-- end-user-doc -->
		 * 
		 * @generated
		 */
		EAttribute TEXT_RANGE__OFFSET = eINSTANCE.getTextRange_Offset();

		/**
		 * The meta object literal for the '<em><b>Length</b></em>' attribute
		 * feature. <!-- begin-user-doc --> <!-- end-user-doc -->
		 * 
		 * @generated
		 */
		EAttribute TEXT_RANGE__LENGTH = eINSTANCE.getTextRange_Length();

		/**
		 * The meta object literal for the '<em><b>Sub Text Ranges</b></em>'
		 * attribute feature. <!-- begin-user-doc --> <!-- end-user-doc -->
		 * 
		 * @generated
		 */
		EAttribute TEXT_RANGE__SUB_TEXT_RANGES = eINSTANCE
				.getTextRange_SubTextRanges();

	}

} // TexttoolsPackage

package org.eventb.texttools.diffmerge;

import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.diff.DiffBuilder;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eventb.emf.core.CorePackage;
import org.eventb.emf.core.impl.StringToAttributeMapEntryImpl;

public class EventBDiffProcessor extends DiffBuilder {

	@Override
	public void referenceChange(Match match, EReference reference,
			EObject value, DifferenceKind kind, DifferenceSource source) {
		// before the EMF update, this code was found in EventBReferencesCheck
		boolean ignore = false;
		// ignore = ignore || reference.isContainment();
		ignore = ignore || reference.isDerived();
		ignore = ignore || reference.isTransient();
		ignore = ignore || reference.isContainer();
		ignore = ignore
				|| reference.eContainer().equals(
						EcorePackage.eINSTANCE.getEGenericType());
		String name = reference.getName();
		// ignore = ignore || "refines".equals(name);
		// ignore = ignore || "sees".equals(name);
		// ignore = ignore || "extends".equals(name);
		ignore = ignore || "annotations".equals(name);
		ignore = ignore || "extensions".equals(name);
		ignore = ignore || "nodes".equals(name);
		// ignore = ignore || "attributes".equals(name); // Cannot ignore
		// generic
		// attributes since camille timestamp is an attribute
		ignore = ignore
				|| reference.eContainer().equals(
						CorePackage.eINSTANCE.getAnnotation());

		// ignore all containments except the org.eventb.texttools attributes
		if ("attributes".equals(name)) {
			if (value instanceof StringToAttributeMapEntryImpl) {
				if (!((StringToAttributeMapEntryImpl) value).getKey()
						.startsWith("org.eventb.texttools")) {
					ignore = true;
				}
			} else {
				ignore = true;
			}
		}

		if (!ignore) {
			super.referenceChange(match, reference, value, kind, source);
		}
	}

	@Override
	public void attributeChange(Match match, EAttribute attribute,
			Object value, DifferenceKind kind, DifferenceSource source) {
		// before the EMF update, this code was found in EventBAttributesCheck

		boolean ignore = false;
		EObject container = attribute.eContainer();
		// remove default ignore transient and derived since some of these are
		// our user visible attributes
		// ignore = ignore || attribute.isTransient();
		// ignore = ignore || attribute.isDerived();

		// ignore contents of string 2 string map entries (e.g. in
		// RodinInternalDetails)
		// FIXME: make this more specific to RodinInternalDetails
		ignore = ignore
				|| (container instanceof ENamedElement && "StringToStringMapEntry"
						.equals(((ENamedElement) container).getName()));
		// ignore contents of Annotations
		// FIXME: make this more specific to RodinInternalDetails
		ignore = ignore
				|| container.equals(CorePackage.eINSTANCE.getAnnotation());
		// ignore reference (instead, the derived attribute 'name' will be
		// shown)
		ignore = ignore
				|| attribute.equals(CorePackage.eINSTANCE
						.getEventBElement_Reference());
		// ignore attributes of Abstract Extension
		ignore = ignore
				|| container.equals(CorePackage.eINSTANCE
						.getAbstractExtension());

		if (!ignore) {
			super.attributeChange(match, attribute, value, kind, source);
		}
	}

	@Override
	public void resourceAttachmentChange(Match match, String uri,
			DifferenceKind kind, DifferenceSource source) {
		super.resourceAttachmentChange(match, uri, kind, source);
	}

}

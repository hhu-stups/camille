/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eventb.emf.core.Attribute;
import org.eventb.emf.core.AttributeType;
import org.eventb.emf.core.CoreFactory;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.texttools.merge.ModelMerge;
import org.eventb.texttools.prettyprint.PrettyPrinter;

public class PersistenceHelper {

	public static IResource getIResource(final Resource resource) {
		final URI uri = resource.getURI();
		if (uri.isPlatformResource()) {
			return ResourcesPlugin.getWorkspace().getRoot().findMember(
					uri.toPlatformString(true));
		}

		return null;
	}

	public static void saveText(final Resource resource,
			final boolean overwrite, final IProgressMonitor monitor)
			throws CoreException {
		try {
			resource.save(Collections.EMPTY_MAP);

			/*
			 * Try to set timestamp to the same as in the annotation. Setting on
			 * both Resource and IResource to be save.
			 */
			final long textTimestamp = getTextTimestamp(resource);
			resource.setTimeStamp(textTimestamp);
			getIResource(resource).setLocalTimeStamp(textTimestamp);
		} catch (final IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					TextToolsPlugin.PLUGIN_ID, "Saving to RodinDB failed", e));
		}
	}

	public static void addTextAnnotation(final Resource resource,
			final String textRepresentation, final long timeStamp)
			throws CoreException {
		final EventBNamedCommentedComponentElement component = getComponent(resource);
		if (component != null) {
			addTextAnnotation(component, textRepresentation, timeStamp);
		} else {
			throw new CoreException(new Status(IStatus.ERROR,
					TextToolsPlugin.PLUGIN_ID,
					"Resource has no EventBComponent"));
		}
	}

	public static void addTextAnnotation(final EventBElement element,
			final String textRepresentation, final long timeStamp) {
		final EMap<String, Attribute> attributes = element.getAttributes();

		// update text representation
		Attribute textAttribute = attributes
				.get(TextToolsPlugin.TYPE_TEXTREPRESENTATION.getId());
		if (textAttribute == null) {
			textAttribute = CoreFactory.eINSTANCE.createAttribute();
			textAttribute.setType(AttributeType.STRING);
			attributes.put(TextToolsPlugin.TYPE_TEXTREPRESENTATION.getId(),
					textAttribute);
		}
		textAttribute.setValue(textRepresentation);

		// update timestamp
		Attribute timeAttribute = attributes
				.get(TextToolsPlugin.TYPE_LASTMODIFIED.getId());
		if (timeAttribute == null) {
			timeAttribute = CoreFactory.eINSTANCE.createAttribute();
			timeAttribute.setType(AttributeType.LONG);
			attributes.put(TextToolsPlugin.TYPE_LASTMODIFIED.getId(),
					timeAttribute);
		}
		timeAttribute.setValue(timeStamp);
	}

	private static void mergeComponents(final EventBNamedCommentedComponentElement oldVersion,
			final EventBNamedCommentedComponentElement newVersion, final IProgressMonitor monitor) {
		try {
			final ModelMerge merge = new ModelMerge(oldVersion, newVersion);
			merge.applyChanges(monitor);
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void mergeRootElement(final Resource resource,
			final EventBNamedCommentedComponentElement newVersion, final IProgressMonitor monitor) {
		final EventBNamedCommentedComponentElement component = getComponent(resource);
		if (component != null) {
			mergeComponents(component, newVersion, monitor);
		} else {
			resource.getContents().add(newVersion);
		}
	}

	public static String loadText(final Resource resource,
			final String linebreak) throws IOException {
		// make sure resource is loaded
		if (!resource.isLoaded()) {
			resource.load(Collections.EMPTY_MAP);
		}

		// check if we have the most recent text representation
		if (isTextUptodate(resource)) {
			// we should find a text representation in the EMF
			final String text = getTextAnnotation(resource);

			if (text != null) {
				return text;
			}
		}

		/*
		 * Reload resource to get latest changes. This is necessary if the model
		 * was already loaded and external changes have occured meanwhile. Then
		 * unloading+loading makes sure the EMF persistence loads those changes.
		 */
		resource.unload();
		resource.load(Collections.EMPTY_MAP);

		// pretty-print the machine/context
		final EventBNamedCommentedComponentElement rootElement = getComponent(resource);
		if (rootElement != null) {
			return getPrettyPrint(rootElement, linebreak);
		} else {
			throw new IOException(
					"Cannot find load Event-B component: No machine/context found");
		}
	}

	public static String getTextAnnotation(final Resource resource) {
		final EMap<String, Attribute> attributes = getAttributesMap(resource);
		if (attributes != null) {
			final Attribute attr = attributes
					.get(TextToolsPlugin.TYPE_TEXTREPRESENTATION.getId());

			if (attr != null) {
				return (String) attr.getValue();
			}
		}

		return null;
	}

	public static EClass getComponentType(final Resource resource) {
		final EventBNamedCommentedComponentElement component = getComponent(resource);

		if (component != null) {
			return component.eClass();
		}

		return null;
	}

	private static EventBNamedCommentedComponentElement getComponent(final Resource resource) {
		final EList<EObject> contents = resource.getContents();
		if (contents.size() > 0 && contents.get(0) instanceof EventBNamedCommentedComponentElement) {
			return (EventBNamedCommentedComponentElement) contents.get(0);
		}

		return null;
	}

	/**
	 * Extracts the timestamp of the latest saved text representation from the
	 * EMF and returns it.
	 *
	 * @param resource
	 * @return timestamp or <code>-1</code> if none is found
	 */
	private static long getTextTimestamp(final Resource resource) {
		final EMap<String, Attribute> attributes = getAttributesMap(resource);
		if (attributes != null) {
			final Attribute attr = attributes
					.get(TextToolsPlugin.TYPE_LASTMODIFIED.getId());
			return attr != null ? (Long) attr.getValue() : -1;
		}

		return -1;
	}

	private static EMap<String, Attribute> getAttributesMap(
			final Resource resource) {
		final EList<EObject> contents = resource.getContents();
		if (contents.size() > 0) {
			final EObject object = contents.get(0);
			if (object instanceof EventBNamedCommentedComponentElement) {
				final EventBNamedCommentedComponentElement component = (EventBNamedCommentedComponentElement) object;
				return component.getAttributes();
			}
		}

		return null;
	}

	/**
	 * Checks if the text representation saved in the EMF is up-to-date. The
	 * timestamps in the EMF and of the underlying file are compared for this
	 * decision.
	 *
	 * @param resource
	 * @return <code>true</code> if there was no external change and the text
	 *         representation is still up-to-date
	 */
	private static boolean isTextUptodate(final Resource resource) {
		final long textTimestamp = getTextTimestamp(resource);

		try {
			final IResource file = getIResource(resource);
			// refresh to get latest timestamp
			file.refreshLocal(IResource.DEPTH_ONE, null);
			final long resourceTimestamp = file.getLocalTimeStamp();

			// easy case: text is newer than resource
			if (textTimestamp >= resourceTimestamp) {
				return true;
			}

			final long diff = resourceTimestamp - textTimestamp;

			// tolerate 50ms offset (time to save file)
			// TODO this is ugly!!!
			if (diff < 50) {
				return true;
			}
		} catch (final CoreException e) {
			TextToolsPlugin.getDefault().getLog().log(
					new Status(IStatus.ERROR, TextToolsPlugin.PLUGIN_ID,
							"Error checking file timestamps", e));
		}

		return false;
	}

	private static String getPrettyPrint(final EventBElement rootElement,
			final String linebreak) {
		final StringBuilder buffer = new StringBuilder();
		new PrettyPrinter(buffer, linebreak, null).prettyPrint(rootElement);

		return buffer.toString();
	}
}

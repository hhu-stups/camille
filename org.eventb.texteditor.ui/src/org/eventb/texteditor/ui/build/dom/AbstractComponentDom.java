/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.dom;

import org.eclipse.emf.ecore.resource.Resource;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;

public abstract class AbstractComponentDom extends AbstractDom implements
		IComponentDom {

	protected boolean initialized = false;
	private boolean initializing = false;
	private final Resource resource;

	protected AbstractComponentDom(final Type type, final IDom parent,
			final Resource resource) {
		super(type, parent);
		this.resource = resource;
	}

	@Override
	public synchronized void reset() {
		if (initialized) {
			super.reset();
			doReset();
			initialized = false;
		}
	}

	public synchronized void resetAndinit() {
		reset();
		init();
	}

	public Resource getResource() {
		return resource;
	}

	protected void checkInitialization() {
		if (!isInitialized()) {
			init();
		}
	}

	private synchronized void init() {
		if (!initialized && !initializing) {
			initializing = true;
			doInitialize();
			initialized = true;
			initializing = false;
		}
	}

	private synchronized boolean isInitialized() {
		return initialized;
	}

	protected void doInitialize() {
		DomBuilder.initializeDOM(this,
				(EventBNamedCommentedComponentElement) getEventBElement(),
				resource);
	}

	protected abstract void doReset();
}

/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package de.avatar.mdp.apis.api;

import org.eclipse.emf.ecore.EObject;
import org.osgi.annotation.versioning.ProviderType;

import de.avatar.mdp.evaluation.EvaluationSummary;

/**
 * This service provider should be responsible for evaluating the instance of a model, 
 * so an EObject
 * 
 * @author ilenia
 * @since Oct 6, 2023
 */
@ProviderType
public interface EObjectEvaluator {

	EvaluationSummary evaluateEObject(EObject eObject);
}

/**
 * Copyright (c) 2012 - 2023 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package de.avatar.mdp.evaluation.component.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.gecko.emf.osgi.example.model.basic.BasicPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.annotation.Property;
import org.osgi.test.common.annotation.config.WithFactoryConfiguration;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.cm.ConfigurationExtension;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import de.avatar.mdp.apis.api.ModelEvaluator;
import de.avatar.mdp.evaluation.EvaluatedTerm;
import de.avatar.mdp.evaluation.EvaluationSummary;


/**
 * See documentation here: 
 * 	https://github.com/osgi/osgi-test
 * 	https://github.com/osgi/osgi-test/wiki
 * Examples: https://github.com/osgi/osgi-test/tree/main/examples
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@ExtendWith(ConfigurationExtension.class)
public class GDPRModelEvaluatorTest {
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "GDPRModelEvaluator",
			location = "?",
			name = "test", 
			properties = {
					@Property(key = "pyScriptBasePath", value = "./data/py/"),
					@Property(key = "outputBasePath", value = "./data/out/")

			})
	public void test(@InjectService ServiceAware<ModelEvaluator> meAware, 
			@InjectService ServiceAware<BasicPackage> packAware) {
		assertThat(meAware).isNotNull();
		ModelEvaluator evaluator = meAware.getService();
		assertThat(evaluator).isNotNull();
		
		assertThat(packAware).isNotNull();
		BasicPackage model = packAware.getService();
		assertThat(model).isNotNull();
		
		EvaluationSummary summary = evaluator.evaluateModel(model);
		assertThat(summary).isNotNull();
		List<EvaluatedTerm> terms = summary.getEvaluatedTerms();
		assertThat(terms).isNotEmpty();
	}

}

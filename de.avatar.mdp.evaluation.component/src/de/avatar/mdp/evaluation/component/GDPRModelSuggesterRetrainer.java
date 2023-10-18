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
package de.avatar.mdp.evaluation.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.avatar.mdp.apis.ModelSuggesterRetrainer;
import de.avatar.mdp.apis.config.SuggesterRetrainerConfig;
import de.avatar.mdp.evaluation.Relevance;
import de.avatar.mdp.evaluation.component.helper.EvaluationHelper;

/**
 * 
 * @author ilenia
 * @since Aug 3, 2023
 */
@Component(name = "GDPRModelSuggesterRetrainer", service = ModelSuggesterRetrainer.class, configurationPid = "GDPRModelSuggesterRetrainer", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GDPRModelSuggesterRetrainer implements ModelSuggesterRetrainer {

	private static final Logger LOGGER = Logger.getLogger(GDPRModelSuggesterRetrainer.class.getName());
	private static final String RETRAIN_SCRIPT_SUFFIX = "_retrain.py";
	
	private PromiseFactory factory = new PromiseFactory(Executors.newFixedThreadPool(4));

	private SuggesterRetrainerConfig config;

	@Activate
	public void activate(SuggesterRetrainerConfig config) {
		this.config = config;
	}

	/* 
	 * (non-Javadoc)
	 * @see de.avatar.mdp.apis.api.ModelSuggesterRetrainer#retrainModelSuggester(java.util.Map)
	 */
	@Override
	public Promise<Boolean> retrainModelSuggester(Map<String, List<Relevance>> relevanceMap) {
		return factory.submit(() -> {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				Map<String, Map<String, String>> docMap = createDocMap(relevanceMap);
				String jsonDocMap = objectMapper.writeValueAsString(docMap);
				EvaluationHelper.executeExternalCmd(LOGGER, "python"+config.pythonVersion(), config.pyScriptBasePath() + config.modelName()+RETRAIN_SCRIPT_SUFFIX, config.basePath(), jsonDocMap);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, String.format("Exception while retraining GDPR suggester for model %s", config.modelName(), e));
				return false;
			}
			return true;
		});
	}

	/**
	 * We want it in the form of 
	 * {"doc1": {"PERSONAL": "RELEVANT", "MEDICAL": "NOT_RELEVANT", "NONE": "NOT_RELEVANT"}, 
	 * "doc2": {"PERSONAL": "POTENTIALLY_RELEVANT", "MEDICAL": "RELEVANT", "NONE": "NOT_RELEVANT"}}
	 * @param relevanceMap
	 * @return
	 */
	private Map<String, Map<String, String>> createDocMap(Map<String, List<Relevance>> relevanceMap) {
		Map<String, Map<String, String>> docMap = new HashMap<>();
		relevanceMap.forEach((doc, relevances) -> {
			Map<String, String> relMap = new HashMap<>();
			relevances.forEach(relevance -> {
				relMap.put(relevance.getCategory(), relevance.getLevel().toString());
			});
			docMap.put(doc, relMap);
		});
		return docMap;
	}

}

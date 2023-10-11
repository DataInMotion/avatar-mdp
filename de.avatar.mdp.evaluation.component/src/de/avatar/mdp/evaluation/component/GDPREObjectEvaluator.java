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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.avatar.mdp.apis.EObjectEvaluator;
import de.avatar.mdp.apis.config.SuggesterConfig;
import de.avatar.mdp.evaluation.EvaluatedTerm;
import de.avatar.mdp.evaluation.Evaluation;
import de.avatar.mdp.evaluation.EvaluationCriteriumType;
import de.avatar.mdp.evaluation.EvaluationSummary;
import de.avatar.mdp.evaluation.MDPEvaluationFactory;
import de.avatar.mdp.evaluation.component.helper.EvaluationHelper;

/**
 * 
 * @author ilenia
 * @since Oct 6, 2023
 */
@Component(name="GDPREObjectEvaluator", service = EObjectEvaluator.class, configurationPid = "GDPREObjectEvaluator", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class GDPREObjectEvaluator implements EObjectEvaluator {

	private static final Logger LOGGER = Logger.getLogger(GDPREObjectEvaluator.class.getName());
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HH:mm:ss.SSS");
	private static final String PREDICTION_SCRIPT_SUFFIX = "_predict.py";
	private static final String ENTITIES_DETECTED_CAT = "ENTITIES_DETECTED";

	private SuggesterConfig config;

	@Activate
	public void activate(SuggesterConfig config) {
		this.config = config;
	}

	/* 
	 * (non-Javadoc)
	 * @see de.avatar.mdp.apis.api.EObjectEvaluator#evaluateEObject(org.eclipse.emf.ecore.EObject)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public EvaluationSummary evaluateEObject(EObject eObject) {
		List<EvaluatedTerm> termsToBeEvaluated = prepareEvaluatedTerms(eObject);

		ObjectMapper objectMapper = new ObjectMapper();
        try {
        	Map<String, List<String>> featureNameDocMap = new LinkedHashMap<>();
        	termsToBeEvaluated
        		.forEach(t -> featureNameDocMap.put(t.getElementClassifierName().concat("::").concat(t.getEvaluatedModelElement().getName()), 
        			t.getEvaluations().stream().map(e -> e.getInput()).toList()));
            String jsonDocMap = objectMapper.writeValueAsString(featureNameDocMap);
            LocalDateTime date = LocalDateTime.now();
            String dateStr = date.format(DATE_TIME_FORMATTER);
            String outFileName = config.outputBasePath() + eObject.eClass().getName() + "_" + config.modelName() + "_" + dateStr +".json";
            EvaluationHelper.executeExternalCmd(LOGGER, "python"+config.pythonVersion(), config.pyScriptBasePath() + config.modelName()+PREDICTION_SCRIPT_SUFFIX, config.modelPath(), outFileName, jsonDocMap);
            Map<String,Object> result = objectMapper.readValue(new File(outFileName), LinkedHashMap.class);
            EvaluationSummary summary = createEvaluationSummary(result, termsToBeEvaluated);
            return summary;
        } catch (IOException e) {
			LOGGER.log(Level.SEVERE, String.format("Exception while making GDPR predicitons for EObject %s", eObject.eClass().getName()), e);
        }
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private EvaluationSummary createEvaluationSummary(Map<String,Object> modelPredictions, List<EvaluatedTerm> termsList) {
		EvaluationSummary summary = MDPEvaluationFactory.eINSTANCE.createEvaluationSummary();
		summary.setEvaluationCriterium(EvaluationCriteriumType.GDPR);
		summary.setEvaluationModelUsed(config.modelName());
		summary.setEvaluationTimestamp(System.currentTimeMillis());
		modelPredictions.forEach((k,v) -> {
			EvaluatedTerm evaluatedTerm = termsList.stream().filter(t -> t.getElementClassifierName().concat("::").concat(t.getEvaluatedModelElement().getName()).equals(k)).findFirst().orElse(null);
			if(evaluatedTerm == null) {
				LOGGER.severe(String.format("No matching term found for evaluated %s", k));
				throw new IllegalStateException(String.format("No matching term found for evaluated %s", k));
			}
			Map<String, Map<String, List<String>>> predictions = (Map<String, Map<String, List<String>>>) v;
			predictions.forEach((doc, pred) -> {
				Evaluation evaluation = evaluatedTerm.getEvaluations().stream().filter(e -> e.getInput().equals(doc)).findFirst().orElse(null);
				if(evaluation == null) {
					LOGGER.severe(String.format("No matching evaluation doc found for evaluated %s", doc));
					throw new IllegalStateException(String.format("No matching evaluation doc found for evaluated %s", doc));
				}
				pred.forEach((category, entities) -> {
					if(ENTITIES_DETECTED_CAT.equals(category)) {
						evaluation.getEntities().addAll(entities);
					} 
				});
			});
			summary.getEvaluatedTerms().add(evaluatedTerm);
		});
		return summary;		
	}

	private List<EvaluatedTerm> prepareEvaluatedTerms(EObject eObject) {
		List<EvaluatedTerm> evaluatedTerms = new LinkedList<>();
		eObject.eClass().getEAllAttributes().forEach(attribute -> {
			evaluatedTerms.addAll(doCreateEvaluatedTerm(attribute, eObject.eClass().getName(), eObject.eGet(attribute)));
		});		
		eObject.eClass().getEAllReferences().forEach(reference -> {
			evaluatedTerms.addAll(doCreateEvaluatedTerm(reference, reference.getEType().getName(), eObject.eGet(reference)));
		});
		return evaluatedTerms;
	}

	private List<EvaluatedTerm> doCreateEvaluatedTerm(EStructuralFeature feature, String eClassifierName, Object elementValue) {
		if(elementValue == null) return Collections.emptyList();
		List<EvaluatedTerm> terms = new ArrayList<>();
		List<String> docs = new ArrayList<>();
		if(feature.getEType() instanceof EDataType eDataType) {
			if(feature.isMany()) {
				List<?> list = (List<?>)elementValue;
				for (Object item : list) {
					docs.add(convertValueToString(eDataType, item));
				}
			} else {
				docs.add(convertValueToString(eDataType, elementValue));
			}
			EvaluatedTerm term = MDPEvaluationFactory.eINSTANCE.createEvaluatedTerm();
			term.setEvaluatedModelElement(feature);
			term.setElementClassifierName(eClassifierName);
			docs.forEach(doc -> {
				Evaluation evaluation = MDPEvaluationFactory.eINSTANCE.createEvaluation();
				evaluation.setInput(doc);
				term.getEvaluations().add(evaluation);
			});
			terms.add(term);
		} else if(feature.getEType() instanceof EClass eClass) {
			eClass.getEAllAttributes().forEach(att -> {
				terms.addAll(doCreateEvaluatedTerm(att, eClass.getName(), ((EObject)elementValue).eGet(att)));
			});
			eClass.getEAllReferences().forEach(ref -> {
				terms.addAll(doCreateEvaluatedTerm(ref, eClass.getName(), ((EObject)elementValue).eGet(ref)));
			});
		} else {
			LOGGER.warning(String.format("Feature %s eType is not of type EDataType, but is %s. Do not know how to convert it.", feature.getName(), feature.getEType()));
		}

		
		return terms;
	}

	private String convertValueToString(EDataType dataType, Object value) {
		return EcoreUtil.convertToString(dataType, value);
	}



}

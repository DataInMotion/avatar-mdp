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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.gecko.emf.osgi.example.model.basic.BasicPackage;
import org.gecko.emf.repository.EMFRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.annotation.Property;
import org.osgi.test.common.annotation.config.WithFactoryConfiguration;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.cm.ConfigurationExtension;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import de.avatar.mdp.apis.api.PRMetaModelService;
import de.avatar.mdp.evaluation.EvaluatedTerm;
import de.avatar.mdp.evaluation.Evaluation;
import de.avatar.mdp.evaluation.EvaluationSummary;
import de.avatar.mdp.evaluation.MDPEvaluationFactory;
import de.avatar.mdp.evaluation.RelevanceLevelType;
import de.avatar.mdp.prmeta.EvaluationCriteriumType;
import de.avatar.mdp.prmeta.EvaluationLevelType;
import de.avatar.mdp.prmeta.PRClassifier;
import de.avatar.mdp.prmeta.PRFeature;
import de.avatar.mdp.prmeta.PRModel;
import de.avatar.mdp.prmeta.PRPackage;

/**
 * 
 * @author ilenia
 * @since Aug 8, 2023
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@ExtendWith(ConfigurationExtension.class)
public class PRMetaModelServiceTest {
	
	
	@BeforeEach
	public void beforeEach() {
		System.out.println("Test");
	}
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "EMFMongoRepositoryConfigurator",
			location = "?",
			name = "avatarmdp", 
			properties = {
					@Property(key = "mongo.instances", value = "avatarmdp"),
					@Property(key = "avatarmdp.baseUris", value = "mongodb://mongodb"),
					@Property(key = "avatarmdp.databases", value = "avatarmdp"),
					@Property(key = "avatarmdp.avatarmdp.repoType", value = "PROTOTYPE")

			})
	public void testServices(@InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware
			,@InjectService(timeout=5000l) ServiceAware<PRMetaModelService> metaModelAware
			) {
		
		assertThat(repoAware).isNotNull();
		EMFRepository repo = repoAware.getService();
		assertThat(repo).isNotNull();
		
		assertThat(metaModelAware).isNotNull();
		PRMetaModelService metaModelService = metaModelAware.getService();
		assertThat(metaModelService).isNotNull();
	}
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "EMFMongoRepositoryConfigurator",
			location = "?",
			name = "avatarmdp", 
			properties = {
					@Property(key = "mongo.instances", value = "avatarmdp"),
					@Property(key = "avatarmdp.baseUris", value = "mongodb://mongodb"),
					@Property(key = "avatarmdp.databases", value = "avatarmdp"),
					@Property(key = "avatarmdp.avatarmdp.repoType", value = "PROTOTYPE")

			})
	public void testSave(@InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware
			,@InjectService(timeout=5000l) ServiceAware<PRMetaModelService> metaModelAware,
			@InjectService ServiceAware<BasicPackage> packAware
			) throws IOException {
	
		
		assertThat(repoAware).isNotNull();
		EMFRepository repo = repoAware.getService();
		assertThat(repo).isNotNull();
		
		assertThat(metaModelAware).isNotNull();
		PRMetaModelService metaModelService = metaModelAware.getService();
		assertThat(metaModelService).isNotNull();
		
		assertThat(packAware).isNotNull();
		BasicPackage model = packAware.getService();
		assertThat(model).isNotNull();
		
		PRModel prModel = metaModelService.createPRModel(createTestEvaluationSummary(), model);		
		metaModelService.savePRModel(prModel);
	}
	
	@Test
	@WithFactoryConfiguration(
			factoryPid = "EMFMongoRepositoryConfigurator",
			location = "?",
			name = "avatarmdp", 
			properties = {
					@Property(key = "mongo.instances", value = "avatarmdp"),
					@Property(key = "avatarmdp.baseUris", value = "mongodb://mongodb"),
					@Property(key = "avatarmdp.databases", value = "avatarmdp"),
					@Property(key = "avatarmdp.avatarmdp.repoType", value = "PROTOTYPE")

			})
	public void testCreation(@InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware
			,@InjectService(timeout=5000l) ServiceAware<PRMetaModelService> metaModelAware,
			@InjectService ServiceAware<BasicPackage> packAware
			) throws IOException {
		
		assertThat(repoAware).isNotNull();
		EMFRepository repo = repoAware.getService();
		assertThat(repo).isNotNull();
		
		assertThat(metaModelAware).isNotNull();
		PRMetaModelService metaModelService = metaModelAware.getService();
		assertThat(metaModelService).isNotNull();
		
		assertThat(packAware).isNotNull();
		BasicPackage model = packAware.getService();
		assertThat(model).isNotNull();
		
		PRModel prModel = metaModelService.createPRModel(createTestEvaluationSummary(), model);
		assertThat(prModel).isNotNull();
		assertThat(prModel.getPackageURI()).isEqualTo(model.getNsURI());
		assertThat(prModel.getPrPackage()).hasSize(2); //by default also the one with EvaluationCriteriumType.NONE is added
		
		PRPackage prPackage1 = null, prPackage2 = null;
		for(PRPackage prPackage : prModel.getPrPackage()) {
			if(prPackage.getEvaluationCriterium().equals(EvaluationCriteriumType.NONE)) {
				prPackage1 = prPackage;
			}
			else if(prPackage.getEvaluationCriterium().equals(EvaluationCriteriumType.GDPR)) {
				prPackage2 = prPackage;
			}
		}
		assertThat(prPackage1).isNotNull();
		assertThat(prPackage2).isNotNull();
		assertThat(prPackage2.getPrClassifier()).hasSize(2);
		assertTrue(prPackage2.getEPackage().eIsProxy());
		EPackage ePackage = (EPackage) EcoreUtil.resolve(prPackage2.getEPackage(), prModel);
		assertThat(ePackage.getNsURI()).isEqualTo(model.getNsURI());
		
		PRClassifier prc1 = null, prc2 = null;
		for(PRClassifier prc : prPackage2.getPrClassifier()) {
			assertTrue(prc.getEClassifier().eIsProxy());
			EClassifier ec = (EClassifier) EcoreUtil.resolve(prc.getEClassifier(), prModel);
			if(ec.getName().equals(model.getAddress().getName())) {
				prc1 = prc;
			} else if(ec.getName().equals(model.getContact().getName())) {
				prc2 = prc;
			}
		}
		assertThat(prc1).isNotNull();
		assertThat(prc2).isNotNull();
		
		assertThat(prc1.getEvaluationLevel()).isEqualTo(EvaluationLevelType.RELEVANT);
		assertThat(prc2.getEvaluationLevel()).isEqualTo(EvaluationLevelType.NOT_RELEVANT);
				
		assertThat(prc1.getPrFeature()).hasSize(1);
		assertThat(prc2.getPrFeature()).hasSize(1);

		PRFeature prf1 = prc1.getPrFeature().get(0);
		assertThat(prf1.getEvaluationLevel()).isEqualTo(EvaluationLevelType.RELEVANT);
		assertTrue(prf1.getFeature().eIsProxy());
		EStructuralFeature sf1 = (EStructuralFeature) EcoreUtil.resolve(prf1.getFeature(), prModel);
		assertThat(sf1.getName()).isEqualTo(BasicPackage.eINSTANCE.getAddress_City().getName());
		
		PRFeature prf2 = prc2.getPrFeature().get(0);
		assertThat(prf2.getEvaluationLevel()).isEqualTo(EvaluationLevelType.NOT_RELEVANT);
		assertTrue(prf2.getFeature().eIsProxy());
		EStructuralFeature sf2 = (EStructuralFeature) EcoreUtil.resolve(prf2.getFeature(), prModel);
		assertThat(sf2.getName()).isEqualTo(model.getContact_Context().getName());		
	}

	private EvaluationSummary createTestEvaluationSummary() {
		EvaluationSummary summary = MDPEvaluationFactory.eINSTANCE.createEvaluationSummary();
		summary.setEvaluationCriterium(de.avatar.mdp.evaluation.EvaluationCriteriumType.GDPR);
		
		EvaluatedTerm term1 = MDPEvaluationFactory.eINSTANCE.createEvaluatedTerm();
		EStructuralFeature f1 = BasicPackage.eINSTANCE.getAddress_City();
		URI uri1 = EcoreUtil.getURI(f1);
		InternalEObject ieo1 = (InternalEObject) f1;
		ieo1.eSetProxyURI(uri1);
		term1.setEvaluatedFeature(f1);
		term1.setFeatureClassifierName(BasicPackage.eINSTANCE.getAddress().getName());
		Evaluation e11 = MDPEvaluationFactory.eINSTANCE.createEvaluation();
		e11.setInput("city");
		e11.setRelevanceLevel(RelevanceLevelType.RELEVANT);
		Evaluation e12 = MDPEvaluationFactory.eINSTANCE.createEvaluation();
		e12.setInput("The city of the customer");
		e12.setRelevanceLevel(RelevanceLevelType.POTENTIALLY_RELEVANT);
		term1.getEvaluations().add(e11);
		term1.getEvaluations().add(e12);
		
		EvaluatedTerm term2 = MDPEvaluationFactory.eINSTANCE.createEvaluatedTerm();
		EStructuralFeature f2 = BasicPackage.eINSTANCE.getContact_Context();
		URI uri2 = EcoreUtil.getURI(f2);
		InternalEObject ieo2 = (InternalEObject) f2;
		ieo2.eSetProxyURI(uri2);
		term2.setEvaluatedFeature(f2);
		term2.setFeatureClassifierName(BasicPackage.eINSTANCE.getContact().getName());
		Evaluation e21 = MDPEvaluationFactory.eINSTANCE.createEvaluation();
		e21.setInput("context");
		e21.setRelevanceLevel(RelevanceLevelType.NOT_RELEVANT);
		term2.getEvaluations().add(e21);

		summary.getEvaluatedTerms().add(term1);
		summary.getEvaluatedTerms().add(term2);
		return summary;
	}
	
}

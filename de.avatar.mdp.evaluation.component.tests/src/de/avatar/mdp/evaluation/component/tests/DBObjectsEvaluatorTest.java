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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

import org.gecko.emf.repository.EMFRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.annotation.Property;
import org.osgi.test.common.annotation.Property.Scalar;
import org.osgi.test.common.annotation.Property.Type;
import org.osgi.test.common.annotation.config.WithFactoryConfiguration;
import org.osgi.test.common.service.ServiceAware;
import org.osgi.test.junit5.cm.ConfigurationExtension;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import de.avatar.mdp.apis.DBObjectsEvaluator;
import de.avatar.mdp.apis.EObjectEvaluator;
import de.avatar.mdp.medicalrecord.MedicalHistory;
import de.avatar.mdp.medicalrecord.MedicalRecord;
import de.avatar.mdp.medicalrecord.MedicalRecordFactory;
import de.avatar.mdp.medicalrecord.PatientInfo;

/**
 * 
 * @author ilenia
 * @since Oct 11, 2023
 */@ExtendWith(BundleContextExtension.class)
 @ExtendWith(ServiceExtension.class)
 @ExtendWith(ConfigurationExtension.class)
 public class DBObjectsEvaluatorTest {

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
	 @WithFactoryConfiguration(
			 factoryPid = "GDPREObjectEvaluator",
			 location = "?",
			 name = "objEvaluator", 
			 properties = {
					 @Property(key = "criterium", value = "GDPR"),
					 @Property(key = "basePath", value = "./data/"),
					 @Property(key = "modelPath", value = "./data/model/"),
					 @Property(key = "pyScriptBasePath", value = "./data/py/ner_fake_pii_generator/"),
					 @Property(key = "outputBasePath", value = "./data/out/"),
					 @Property(key = "modelName", value = "ner_fake_pii_generator")

			 })
	 @WithFactoryConfiguration(
			 factoryPid = "DBObjectsEvaluator",
			 location = "?",
			 name = "dbEvaluator", 
			 properties = {
					 @Property(key = "evaluation.out.folder", value = "./data/model/")
			 })
	 public void testServices(
			 @InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware,
			 @InjectService ServiceAware<EObjectEvaluator> meAware, 
			 @InjectService ServiceAware<DBObjectsEvaluator> dbeAware) {

		 assertThat(repoAware).isNotNull();
		 EMFRepository repo = repoAware.getService();
		 assertThat(repo).isNotNull();

		 assertThat(meAware).isNotNull();
		 EObjectEvaluator eObjevaluator = meAware.getService();
		 assertThat(eObjevaluator).isNotNull();

		 assertThat(dbeAware).isNotNull();
		 DBObjectsEvaluator dbEvaluator = dbeAware.getService();
		 assertThat(dbEvaluator).isNotNull();
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
	 @WithFactoryConfiguration(
			 factoryPid = "GDPREObjectEvaluator",
			 location = "?",
			 name = "objEvaluator", 
			 properties = {
					 @Property(key = "criterium", value = "GDPR"),
					 @Property(key = "basePath", value = "./data/"),
					 @Property(key = "modelPath", value = "./data/model/"),
					 @Property(key = "pyScriptBasePath", value = "./data/py/ner_fake_pii_generator/"),
					 @Property(key = "outputBasePath", value = "./data/out/"),
					 @Property(key = "modelName", value = "ner_fake_pii_generator")

			 })
	 @WithFactoryConfiguration(
			 factoryPid = "DBObjectsEvaluator",
			 location = "?",
			 name = "dbEvaluator", 
			 properties = {
					 @Property(key = "evaluation.out.folder", value = "./data/out/"),
					 @Property(key = "eClass.uris.to.evaluate", value = {"http://avatar.de/mdp/medical_record_example/1.0.0#//MedicalRecord"}, type = Type.Array, scalar = Scalar.String)
			 })
	 public void testDBEvaluatorDefaultConfig(
			 @InjectService(timeout = 5000l, filter = "(repo_id=avatarmdp.avatarmdp)") ServiceAware<EMFRepository> repoAware,
			 @InjectService ServiceAware<EObjectEvaluator> meAware, 
			 @InjectService ServiceAware<DBObjectsEvaluator> dbeAware) throws InterruptedException {

		 assertThat(repoAware).isNotNull();
		 EMFRepository repo = repoAware.getService();
		 assertThat(repo).isNotNull();

		 assertThat(meAware).isNotNull();
		 EObjectEvaluator eObjevaluator = meAware.getService();
		 assertThat(eObjevaluator).isNotNull();

		 assertThat(dbeAware).isNotNull();
		 DBObjectsEvaluator dbEvaluator = dbeAware.getService();
		 assertThat(dbEvaluator).isNotNull();
		 
		 MedicalRecord medicalRecord = createTestMedicalRecord();
		 repo.save(medicalRecord);
		 
		 dbEvaluator.evaluate();
		 
		 Thread.sleep(15000);
		 
		 String fileName = "data/out/avatarmdp.avatarmdp_MedicalRecord_"+medicalRecord.getId()+".json";
		 File file = new File(fileName);
		 assertTrue(file.exists());
		 
		 repo.delete(medicalRecord);
	 }
	 
	 @Test
	 @WithFactoryConfiguration(
			 factoryPid = "EMFMongoRepositoryConfigurator",
			 location = "?",
			 name = "test", 
			 properties = {
					 @Property(key = "mongo.instances", value = "test"),
					 @Property(key = "test.baseUris", value = "mongodb://mongodb"),
					 @Property(key = "test.databases", value = "test"),
					 @Property(key = "test.test.repoType", value = "PROTOTYPE")

			 })
	 @WithFactoryConfiguration(
			 factoryPid = "GDPREObjectEvaluator",
			 location = "?",
			 name = "objEvaluator", 
			 properties = {
					 @Property(key = "criterium", value = "GDPR"),
					 @Property(key = "basePath", value = "./data/"),
					 @Property(key = "modelPath", value = "./data/model/"),
					 @Property(key = "pyScriptBasePath", value = "./data/py/ner_fake_pii_generator/"),
					 @Property(key = "outputBasePath", value = "./data/out/"),
					 @Property(key = "modelName", value = "ner_fake_pii_generator")

			 })
	 @WithFactoryConfiguration(
			 factoryPid = "DBObjectsEvaluator",
			 location = "?",
			 name = "dbEvaluator", 
			 properties = {
					 @Property(key = "repositorySO.target", value = "(repo_id=test.test)"),
					 @Property(key = "evaluation.out.folder", value = "./data/out/"),
					 @Property(key = "eClass.uris.to.evaluate", value = {"http://avatar.de/mdp/medical_record_example/1.0.0#//MedicalRecord"}, type = Type.Array, scalar = Scalar.String)
			 })
	 public void testDBEvaluatorDifferentRepoFilter(
			 @InjectService(timeout = 5000l, filter = "(repo_id=test.test)") ServiceAware<EMFRepository> repoAware,
			 @InjectService ServiceAware<EObjectEvaluator> meAware, 
			 @InjectService ServiceAware<DBObjectsEvaluator> dbeAware) throws InterruptedException {

		 assertThat(repoAware).isNotNull();
		 EMFRepository repo = repoAware.getService();
		 assertThat(repo).isNotNull();

		 assertThat(meAware).isNotNull();
		 EObjectEvaluator eObjevaluator = meAware.getService();
		 assertThat(eObjevaluator).isNotNull();

		 assertThat(dbeAware).isNotNull();
		 DBObjectsEvaluator dbEvaluator = dbeAware.getService();
		 assertThat(dbEvaluator).isNotNull();
		 
		 MedicalRecord medicalRecord = createTestMedicalRecord();
		 repo.save(medicalRecord);
		 
		 dbEvaluator.evaluate();
		 
		 Thread.sleep(15000);
		 
		 String fileName = "data/out/test.test_MedicalRecord_"+medicalRecord.getId()+".json";
		 File file = new File(fileName);
		 assertTrue(file.exists());
		 
		 repo.delete(medicalRecord);
	 }
	 
	 @AfterEach
	 public void afterEach() throws IOException {
		 Path folder = Path.of("data/out/");
		 Files.list(folder).forEach(p -> {
			try {
				if(Files.exists(p)) Files.delete(p);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		 });
	 }
	 
	 

	 
	 private MedicalRecord createTestMedicalRecord() {
		 MedicalRecord medicalRecord = MedicalRecordFactory.eINSTANCE.createMedicalRecord();
		 medicalRecord.setId(UUID.randomUUID().toString());
		 PatientInfo patientInfo = MedicalRecordFactory.eINSTANCE.createPatientInfo();
		 patientInfo.setName("John Doe");
		 patientInfo.setAddress("Camsdorfer Str. 36");
		 patientInfo.setBirthDate(new Date());
		 medicalRecord.setPatientInfo(patientInfo);
		 MedicalHistory medicalHistory = MedicalRecordFactory.eINSTANCE.createMedicalHistory();
		 medicalHistory.setAllergies("none");
		 medicalHistory.setVaccinations("I got covid-19 vaccination");
		 medicalRecord.setMedicalHistory(medicalHistory);
		 return medicalRecord;
	 }
 }

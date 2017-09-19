/*
 * Copyright 2017 Barclays Africa Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.persistence.atlas

import java.io.{File, FileWriter, IOException}
import java.util.Properties

import org.apache.atlas.ApplicationProperties
import org.apache.commons.configuration.Configuration
import za.co.absa.spline.common.ARMImplicits
import za.co.absa.spline.persistence.api._


/**
  * The object contains static information about settings needed for initialization of the AtlasPersistenceFactory class.
  */
object AtlasPersistenceFactory {
  val atlasPropertyPrefix = "atlas"
  val atlasConfigurationDirKey = ApplicationProperties.ATLAS_CONFIGURATION_DIRECTORY_PROPERTY
  val atlasTemporaryConfigurationFileName = ApplicationProperties.APPLICATION_PROPERTIES
}

/**
  * The class represents a factory creating Atlas persistence layers for all main data lineage entities.
  *
  * @param configuration A source of settings
  */
class AtlasPersistenceFactory(configuration: Configuration) extends PersistenceFactory(configuration) {

  import AtlasPersistenceFactory._

  import scala.collection.JavaConverters._

  createAtlasTemporaryConfigurationFile()

  def createTempDirectory(): File = {
    val temp = File.createTempFile("temp", System.nanoTime().toString)
    if (!temp.delete) throw new IOException("Could not delete temp file: " + temp.getAbsolutePath)
    if (!temp.mkdir) throw new IOException("Could not create temp directory: " + temp.getAbsolutePath)
    temp
  }

  private def createAtlasTemporaryConfigurationFile(): Unit = {
    val atlasConfTempDir = createTempDirectory()
    val atlasConfTempFile = new File(atlasConfTempDir, atlasTemporaryConfigurationFileName)
    atlasConfTempFile.deleteOnExit()
    atlasConfTempDir.deleteOnExit()

    System.setProperty(atlasConfigurationDirKey, atlasConfTempDir.getAbsolutePath)

    val atlasProps = new Properties() {
      (configuration getKeys atlasPropertyPrefix).asScala.foreach(key =>
        setProperty(key, configuration getString key))
    }

    import ARMImplicits._
    for (fw <- new FileWriter(atlasConfTempFile)) {
      atlasProps.store(fw, "DO NOT MODIFY! The content is automatically generated by Spline.")
    }
  }

  /**
    * The method creates a persistence layer for the [[za.co.absa.spline.model.DataLineage DataLineage]] entity.
    *
    * @return A persistence layer for the [[za.co.absa.spline.model.DataLineage DataLineage]] entity
    */
  override def createDataLineagePersistor(): DataLineagePersistor = new AtlasDataLineagePersistor

  /**
    * The method creates a persistence layer for the [[za.co.absa.spline.model.Execution Execution]] entity.
    *
    * @return A persistence layer for the [[za.co.absa.spline.model.Execution Execution]] entity
    */
  override def createExecutionPersistor(): ExecutionPersistor = new NopExecutionPersistor
}

package com.github.imflog.schema.registry.compatibility

import com.github.imflog.schema.registry.BaseTaskAction
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import io.confluent.kafka.schemaregistry.rest.exceptions.Errors
import org.gradle.api.logging.Logging
import java.io.File
import java.io.IOException

class CompatibilityTaskAction(
    client: SchemaRegistryClient,
    rootDir: File,
    private val subjects: List<CompatibilitySubject>
) : BaseTaskAction(client, rootDir) {

    private val logger = Logging.getLogger(CompatibilityTaskAction::class.java)

    fun run(): Int {
        var errorCount = 0
        for ((subject, path, type, dependencies) in subjects) {
            logger.debug("Loading schema for subject($subject) from $path.")
            val isCompatible = try {
                val parsedSchema = parseSchemaFromFile(path, type, dependencies)
                client.testCompatibility(subject, parsedSchema)
            } catch (ioEx: IOException) {
                logger.error("", ioEx)
                false
            } catch (restEx: RestClientException) {
                // If the subject does not exist, it is compatible
                if (restEx.errorCode == Errors.SUBJECT_NOT_FOUND_ERROR_CODE) {
                    true
                } else {
                    logger.error("", restEx)
                    false
                }
            }
            if (isCompatible) {
                logger.info("Schema $path is compatible with subject: $subject")
            } else {
                logger.error("Schema $path is not compatible with subject: $subject")
                errorCount++
            }
        }
        return errorCount
    }
}

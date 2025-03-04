/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.regex.RegularExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class PatternValidator extends BaseJsonValidator {
    private static final Logger logger = LoggerFactory.getLogger(PatternValidator.class);
    private String pattern;
    private RegularExpression compiledPattern;

    public PatternValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.PATTERN, validationContext);

        this.pattern = Optional.ofNullable(schemaNode).filter(JsonNode::isTextual).map(JsonNode::textValue).orElse(null);
        try {
            this.compiledPattern = RegularExpression.compile(pattern, validationContext);
        } catch (RuntimeException e) {
            e.setStackTrace(new StackTraceElement[0]);
            logger.error("Failed to compile pattern '{}': {}", pattern, e.getMessage());
            throw e;
        }
        this.validationContext = validationContext;
        parseErrorCode(getValidatorType().getErrorCodeKey());
    }

    private boolean matches(String value) {
        return compiledPattern.matches(value);
    }

    public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);

        JsonType nodeType = TypeFactory.getValueNodeType(node, this.validationContext.getConfig());
        if (nodeType != JsonType.STRING) {
            return Collections.emptySet();
        }

        try {
            if (!matches(node.asText())) {
                return Collections.singleton(buildValidationMessage(at, pattern));
            }
        } catch (RuntimeException e) {
            logger.error("Failed to apply pattern '{}' at {}: {}", pattern, at, e.getMessage());
        }

        return Collections.emptySet();
    }
}

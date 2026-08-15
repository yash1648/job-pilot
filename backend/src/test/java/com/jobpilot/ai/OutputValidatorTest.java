package com.jobpilot.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutputValidatorTest {

    @Test
    void acceptsPresentRequiredField() {
        ResponseSchema schema = new ResponseSchema(new String[]{"skills"});
        assertDoesNotThrow(() -> OutputValidator.parseAndValidate("{\"skills\":[\"java\"]}", schema));
    }

    @Test
    void rejectsMissingRequiredField() {
        ResponseSchema schema = new ResponseSchema(new String[]{"skills"});
        assertThrows(AiOutputInvalidException.class,
                () -> OutputValidator.parseAndValidate("{\"other\":1}", schema));
    }

    @Test
    void rejectsNonJson() {
        assertThrows(AiOutputInvalidException.class,
                () -> OutputValidator.parseAndValidate("not json", null));
    }
}

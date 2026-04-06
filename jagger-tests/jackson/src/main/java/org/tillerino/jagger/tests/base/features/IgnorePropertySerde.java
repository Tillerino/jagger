package org.tillerino.jagger.tests.base.features;

import com.fasterxml.jackson.core.JsonGenerator;
import org.tillerino.jagger.annotations.JsonOutput;
import org.tillerino.jagger.tests.model.features.IgnorePropertyModel.*;

public interface IgnorePropertySerde {
    @JsonOutput
    void writeFieldWithGetter(JsonIgnoreOnFieldWithGetter obj, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeFieldAndGetter(JsonIgnoreOnFieldAndGetter obj, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeFieldWithoutGetter(JsonIgnoreOnFieldWithoutGetter obj, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeGetter(JsonIgnoreOnGetter obj, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeRecordComponent(JsonIgnoreOnRecordComponent obj, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeChildInheritsParentIgnore(ChildInheritsParentIgnore obj, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeChildImplementsParentInterface(ChildImplementsParentInterface obj, JsonGenerator out) throws Exception;

    @JsonOutput
    void writeChildInheritsGrandparentIgnore(ChildInheritsGrandparentIgnore obj, JsonGenerator out) throws Exception;
}

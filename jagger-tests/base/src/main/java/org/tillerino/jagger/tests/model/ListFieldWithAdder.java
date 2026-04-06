package org.tillerino.jagger.tests.model;

import java.util.List;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class ListFieldWithAdder {
    List<String> things;

    public List<String> getThings() {
        return things;
    }

    public void addThing(String s) {
        // This is not called, but here because it might get picked up instead of the setter.
        things.add(s);
    }

    public void setThings(List<String> things) {
        this.things = things;
    }
}

package org.tillerino.jagger.tests.variants;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import java.util.ArrayList;
import java.util.List;
import org.tillerino.jagger.tests.Replacement;
import org.tillerino.jagger.tests.variants.GenerateVariants.VariantValue;
import org.tillerino.jagger.tests.variants.GenerateVariants.VariantValue.ReplacementValue;

/**
 * Spaghetti code to extract a {@link VariantsKarthesianProduct} from any shape of annotations on an object. After
 * running, the result can be taken from {@link #karth}.
 */
class VariantsExtractor extends GenericVisitorAdapter<Void, Void> {
    List<List<VariantValue>> karth = null;
    List<ReplacementValue> replacements = null;
    String regex;
    String replacement;
    String string;

    @Override
    public Void visit(NormalAnnotationExpr n, Void arg) {
        if (this.visit((AnnotationExpr) n, arg)) {
            return null;
        }
        return super.visit(n, arg);
    }

    @Override
    public Void visit(SingleMemberAnnotationExpr n, Void arg) {
        if (this.visit((AnnotationExpr) n, arg)) {
            return null;
        }
        return super.visit(n, arg);
    }

    private boolean visit(AnnotationExpr n, Void arg) {
        if (n.getName().getIdentifier().equals(VariantsKarthesianProduct.class.getSimpleName())) {
            if (karth != null) {
                throw new IllegalStateException();
            }
            karth = new ArrayList<>();
            n.getChildNodes().forEach(c -> c.accept(this, arg));
            return true;
        }

        if (n.getName().getIdentifier().equals(Variants.class.getSimpleName())) {
            if (karth == null) {
                karth = new ArrayList<>();
            }
            karth.add(new ArrayList<>());
            n.getChildNodes().forEach(c -> c.accept(this, arg));
            return true;
        }

        if (n.getName().getIdentifier().equals(Variant.class.getSimpleName())) {
            replacements = new ArrayList<>();
            n.getChildNodes().forEach(c -> c.accept(this, arg));
            if (karth == null) {
                karth = new ArrayList<>();
            }
            if (karth.isEmpty()) {
                karth.add(new ArrayList<>());
            }
            karth.get(karth.size() - 1).add(new VariantValue(replacements));
            return true;
        }

        if (n.getName().getIdentifier().equals(Replacement.class.getSimpleName())) {
            n.getChildNodes().forEach(c -> c.accept(this, arg));
            replacements.add(new ReplacementValue(regex, replacement));
            return true;
        }

        return false;
    }

    @Override
    public Void visit(MemberValuePair n, Void arg) {
        if (n.getName().getIdentifier().equals("regex")) {
            super.visit(n, arg);
            regex = string;
            return null;
        }
        if (n.getName().getIdentifier().equals("replacement")) {
            super.visit(n, arg);
            replacement = string;
            return null;
        }
        return super.visit(n, arg);
    }

    @Override
    public Void visit(StringLiteralExpr n, Void arg) {
        string = n.asString();
        return null;
    }
}

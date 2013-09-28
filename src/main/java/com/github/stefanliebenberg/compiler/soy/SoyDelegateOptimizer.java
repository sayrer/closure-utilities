package com.github.stefanliebenberg.compiler.soy;


import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.*;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Johannes Nel
 * @author Stefan Liebenberg
 */
public class SoyDelegateOptimizer implements CompilerPass {

    private Compiler compiler;

    private final DelegateFinder delegateFinder = new DelegateFinder();

    private final DelegateOptimiser delegateOptimizer = new DelegateOptimiser();


    public SoyDelegateOptimizer(final Compiler compiler) {
        this.compiler = compiler;
    }

    private final HashMap<String, Double> dataMap = new HashMap<String,
            Double>();

    protected void reset() {
        dataMap.clear();
    }

    @Override
    public void process(Node externs, Node root) {
        reset();
        NodeTraversal.traverse(compiler, root, delegateFinder);
        NodeTraversal.traverse(compiler, root, delegateOptimizer);
    }

    private class DelegateFinder extends NodeTraversal
            .AbstractPostOrderCallback {

        @Override
        public void visit(final NodeTraversal t, final Node n,
                          final Node parent) {
            if (isDelegateCallNode(n)) {
                Double priority = getPriority(n);
                String key = getDelegateId(n);
                Double currentPriorityInMap = dataMap.get(key);
                if (currentPriorityInMap == null ||
                        currentPriorityInMap < priority) {
                    dataMap.put(key, priority);
                }

            }
        }
    }

    public class DelegateOptimiser extends NodeTraversal
            .AbstractPostOrderCallback {

        @Override
        public void visit(final NodeTraversal t,
                          final Node n,
                          final Node parent) {
            if (isDelegateCallNode(n)) {
                Double priority = getPriority(n);
                String key = getDelegateId(n);
                Double highestPriorityInMap = dataMap.get(key);
                if (priority < highestPriorityInMap) {
                    parent.detachFromParent();
                    compiler.reportCodeChange();
                }
            }
        }
    }

    static private String getName(final Node node) {
        return node.getFirstChild().getNext().getLastChild()
                .getString();
    }

    static private String getVariant(final Node node) {
        return node.getChildAtIndex(1).getNext().getString();
    }

    static private Double getPriority(final Node node) {
        return node.getChildAtIndex(3).getDouble();
    }

    static private String getKey(final String name, final String variant) {
        return name + ":" + variant;
    }

    static private String getDelegateId(final Node node) {
        return getKey(getName(node), getVariant(node));
    }

    static private final String DELEGATE_FN_NAME = "soy.$$registerDelegateFn";

    static private Boolean isDelegateCallNode(final Node node) {
        return node.getType() == Token.CALL && node.getFirstChild()
                .getQualifiedName().equals(DELEGATE_FN_NAME);
    }


    public static void addToCompile(Compiler compiler,
                                    CompilerOptions compilerOptions) {
        Multimap<CustomPassExecutionTime, CompilerPass> customPasses =
                compilerOptions.customPasses;
        if (customPasses == null) {
            customPasses = LinkedListMultimap.create();
        }
        customPasses.put(CustomPassExecutionTime.BEFORE_CHECKS,
                new SoyDelegateOptimizer(compiler));
        compilerOptions.setCustomPasses(customPasses);
    }
}

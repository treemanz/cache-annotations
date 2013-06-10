/**
 * @(#)TwoTuple.java, 2011-4-1. Copyright 2011 Netease, Inc. All rights
 *                    reserved. NETEASE PROPRIETARY/CONFIDENTIAL. Use is subject
 *                    to license terms.
 */
package cn.gd.gz.treemanz.toolbox.cache;

/**
 * @author chenjian
 */
public class TwoTuple<A, B> {
    private A first;

    private B second;

    public TwoTuple(A a, B b) {
        first = a;
        second = b;
    }

    /**
     * @return the first
     */
    public A getFirst() {
        return first;
    }

    /**
     * @return the second
     */
    public B getSecond() {
        return second;
    }

    /**
     * @param first
     *            the first to set
     */
    public void setFirst(A first) {
        this.first = first;
    }

    /**
     * @param second
     *            the second to set
     */
    public void setSecond(B second) {
        this.second = second;
    }

    @Override
    public String toString() {
        return "{\"first\":" + first + ",\"second\":" + second + "}";
    }
}

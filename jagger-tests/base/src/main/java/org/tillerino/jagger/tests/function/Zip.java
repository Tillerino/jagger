package org.tillerino.jagger.tests.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.apache.commons.lang3.function.TriFunction;

public class Zip {
    public static <A, B, T> List<T> instantiate2(List<A> as, List<B> bs, BiFunction<A, B, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < Math.max(as.size(), bs.size()); i++) {
            result.add(constructor.apply(as.get(i % as.size()), bs.get(i % bs.size())));
        }
        return result;
    }

    // LLM, take the wheel :D

    public static <A, B, C, T> List<T> instantiate3(
            List<A> as, List<B> bs, List<C> cs, TriFunction<A, B, C, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < Math.max(Math.max(as.size(), bs.size()), cs.size()); i++) {
            result.add(constructor.apply(as.get(i % as.size()), bs.get(i % bs.size()), cs.get(i % cs.size())));
        }
        return result;
    }

    public static <A, B, C, D, T> List<T> instantiate4(
            List<A> as, List<B> bs, List<C> cs, List<D> ds, Function4<A, B, C, D, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0; i < Math.max(Math.max(Math.max(as.size(), bs.size()), cs.size()), ds.size()); i++) {
            result.add(constructor.apply(
                    as.get(i % as.size()), bs.get(i % bs.size()), cs.get(i % cs.size()), ds.get(i % ds.size())));
        }
        return result;
    }

    public static <A, B, C, D, E, T> List<T> instantiate5(
            List<A> as, List<B> bs, List<C> cs, List<D> ds, List<E> es, Function5<A, B, C, D, E, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0;
                i < Math.max(Math.max(Math.max(Math.max(as.size(), bs.size()), cs.size()), ds.size()), es.size());
                i++) {
            result.add(constructor.apply(
                    as.get(i % as.size()),
                    bs.get(i % bs.size()),
                    cs.get(i % cs.size()),
                    ds.get(i % ds.size()),
                    es.get(i % es.size())));
        }
        return result;
    }

    public static <A, B, C, D, E, F, T> List<T> instantiate6(
            List<A> as,
            List<B> bs,
            List<C> cs,
            List<D> ds,
            List<E> es,
            List<F> fs,
            Function6<A, B, C, D, E, F, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0;
                i
                        < Math.max(
                                Math.max(
                                        Math.max(Math.max(Math.max(as.size(), bs.size()), cs.size()), ds.size()),
                                        es.size()),
                                fs.size());
                i++) {
            result.add(constructor.apply(
                    as.get(i % as.size()),
                    bs.get(i % bs.size()),
                    cs.get(i % cs.size()),
                    ds.get(i % ds.size()),
                    es.get(i % es.size()),
                    fs.get(i % fs.size())));
        }
        return result;
    }

    public static <A, B, C, D, E, F, G, T> List<T> instantiate7(
            List<A> as,
            List<B> bs,
            List<C> cs,
            List<D> ds,
            List<E> es,
            List<F> fs,
            List<G> gs,
            Function7<A, B, C, D, E, F, G, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0;
                i
                        < Math.max(
                                Math.max(
                                        Math.max(
                                                Math.max(
                                                        Math.max(Math.max(as.size(), bs.size()), cs.size()), ds.size()),
                                                es.size()),
                                        fs.size()),
                                gs.size());
                i++) {
            result.add(constructor.apply(
                    as.get(i % as.size()),
                    bs.get(i % bs.size()),
                    cs.get(i % cs.size()),
                    ds.get(i % ds.size()),
                    es.get(i % es.size()),
                    fs.get(i % fs.size()),
                    gs.get(i % gs.size())));
        }
        return result;
    }

    public static <A, B, C, D, E, F, G, H, T> List<T> instantiate8(
            List<A> as,
            List<B> bs,
            List<C> cs,
            List<D> ds,
            List<E> es,
            List<F> fs,
            List<G> gs,
            List<H> hs,
            Function8<A, B, C, D, E, F, G, H, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0;
                i
                        < Math.max(
                                Math.max(
                                        Math.max(
                                                Math.max(
                                                        Math.max(
                                                                Math.max(Math.max(as.size(), bs.size()), cs.size()),
                                                                ds.size()),
                                                        es.size()),
                                                fs.size()),
                                        gs.size()),
                                hs.size());
                i++) {
            result.add(constructor.apply(
                    as.get(i % as.size()),
                    bs.get(i % bs.size()),
                    cs.get(i % cs.size()),
                    ds.get(i % ds.size()),
                    es.get(i % es.size()),
                    fs.get(i % fs.size()),
                    gs.get(i % gs.size()),
                    hs.get(i % hs.size())));
        }
        return result;
    }

    public static <A, B, C, D, E, F, G, H, I, T> List<T> instantiate9(
            List<A> as,
            List<B> bs,
            List<C> cs,
            List<D> ds,
            List<E> es,
            List<F> fs,
            List<G> gs,
            List<H> hs,
            List<I> is,
            Function9<A, B, C, D, E, F, G, H, I, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0;
                i
                        < Math.max(
                                Math.max(
                                        Math.max(
                                                Math.max(
                                                        Math.max(
                                                                Math.max(
                                                                        Math.max(
                                                                                Math.max(as.size(), bs.size()),
                                                                                cs.size()),
                                                                        ds.size()),
                                                                es.size()),
                                                        fs.size()),
                                                gs.size()),
                                        hs.size()),
                                is.size());
                i++) {
            result.add(constructor.apply(
                    as.get(i % as.size()),
                    bs.get(i % bs.size()),
                    cs.get(i % cs.size()),
                    ds.get(i % ds.size()),
                    es.get(i % es.size()),
                    fs.get(i % fs.size()),
                    gs.get(i % gs.size()),
                    hs.get(i % hs.size()),
                    is.get(i % is.size())));
        }
        return result;
    }

    public static <A, B, C, D, E, F, G, H, I, J, T> List<T> instantiate10(
            List<A> as,
            List<B> bs,
            List<C> cs,
            List<D> ds,
            List<E> es,
            List<F> fs,
            List<G> gs,
            List<H> hs,
            List<I> is,
            List<J> js,
            Function10<A, B, C, D, E, F, G, H, I, J, T> constructor) {
        List<T> result = new ArrayList<>();
        for (int i = 0;
                i
                        < Math.max(
                                Math.max(
                                        Math.max(
                                                Math.max(
                                                        Math.max(
                                                                Math.max(
                                                                        Math.max(
                                                                                Math.max(
                                                                                        Math.max(as.size(), bs.size()),
                                                                                        cs.size()),
                                                                                ds.size()),
                                                                        es.size()),
                                                                fs.size()),
                                                        gs.size()),
                                                hs.size()),
                                        is.size()),
                                js.size());
                i++) {
            result.add(constructor.apply(
                    as.get(i % as.size()),
                    bs.get(i % bs.size()),
                    cs.get(i % cs.size()),
                    ds.get(i % ds.size()),
                    es.get(i % es.size()),
                    fs.get(i % fs.size()),
                    gs.get(i % gs.size()),
                    hs.get(i % hs.size()),
                    is.get(i % is.size()),
                    js.get(i % js.size())));
        }
        return result;
    }
}
